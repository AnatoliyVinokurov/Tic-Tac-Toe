package com.anatoliyvinokurov.tictactoe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import android.media.MediaPlayer


//ca-app-pub-3940256099942544/1033173712
const val AD_UNIT_ID = "ca-app-pub-7477627546717786/9246410027"

class MainActivity : AppCompatActivity() {
	private var mediaPlayer1: MediaPlayer? = null
    private var mediaPlayer2: MediaPlayer? = null
    private var activePlayer = 0
    private var gameState = IntArray(9) { 2 }
    private val winningPositions = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5),
        intArrayOf(6, 7, 8), intArrayOf(0, 3, 6), intArrayOf(1, 4, 7),
        intArrayOf(2, 5, 8), intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
    )
    private var gameActive = true
    private var interstitialAd: InterstitialAd? = null
    private var adIsLoading: Boolean = false
    private var numberOfGame = 0
    private lateinit var restart: Button
    private lateinit var playTheGame: TextView
    private lateinit var score: TextView
    private var gameResult: String? = null
    private var currentPlayerName = "Player 1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeViews()
        // Инициализация медиаплееров для звуков
        mediaPlayer1 = MediaPlayer.create(this, R.raw.music1)
        mediaPlayer2 = MediaPlayer.create(this, R.raw.music2)
        MobileAds.initialize(this) {}
        restart.setOnClickListener { resetGameAndShowAd() }
    }

    private fun initializeViews() {
        restart = findViewById(R.id.button)
        playTheGame = findViewById(R.id.textView2)
        score = findViewById(R.id.textView)
        playTheGame.text = "$currentPlayerName's turn"
    }

    private fun resetGameAndShowAd() {
        resetGame()
        if(numberOfGame >= 4){
        showInterstitialAd()
        numberOfGame=0
        }else{
        numberOfGame++;
        }
        
    }

    private fun resetGame() {
        clearBoard()
        gameState = IntArray(9) { 2 }
        gameActive = true
        activePlayer = 0
        gameResult = null
        currentPlayerName = "Player 1"
        playTheGame.visibility = View.VISIBLE
        restart.visibility = View.INVISIBLE
        score.visibility = View.INVISIBLE
    }

    private fun clearBoard() {
        val gridLayout = findViewById<GridLayout>(R.id.gridlayout)
        for (i in 0 until gridLayout.childCount) {
            val imageView = gridLayout.getChildAt(i) as ImageView
            imageView.setImageDrawable(null)
        }
    }

    fun dropIn(view: View) {
        val counter = view as ImageView
        val tappedCounter = counter.tag.toString().toInt()
        if (gameState[tappedCounter] == 2 && gameActive) {
            gameState[tappedCounter] = activePlayer
            // Воспроизведение звуков в зависимости от активного игрока
            if (activePlayer == 0) {
                mediaPlayer1?.start()
            } else {
                mediaPlayer2?.start()
            }
            counter.translationY = -1500f
            setCounterImageAndSwitchPlayer(counter)
            counter.animate().translationYBy(1500f).rotation(36000f).setDuration(300)
            checkForWin()
        }
    }

    private fun setCounterImageAndSwitchPlayer(counter: ImageView) {
        if (activePlayer == 0) {
            counter.setImageResource(R.drawable.tictcx)
            activePlayer = 1
            currentPlayerName = "Player 2"
        } else {
            counter.setImageResource(R.drawable.tictactoe_o)
            activePlayer = 0
            currentPlayerName = "Player 1"
        }
    }

    private fun checkForWin() {
        var allCellsFilled = true
        for (i in gameState) {
            if (i == 2) {
                allCellsFilled = false
                break
            }
        }

        var winningLine: IntArray? = null

        for (winningPosition in winningPositions) {
            if (gameState[winningPosition[0]] == gameState[winningPosition[1]] &&
                gameState[winningPosition[1]] == gameState[winningPosition[2]] &&
                gameState[winningPosition[1]] != 2
            ) {
                gameActive = false
                winningLine = winningPosition
                break
            }
        }

        if (winningLine != null) {
            gameResult = if (activePlayer == 1) "Player 1 Wins" else "Player 2 Wins"
        } else if (allCellsFilled && gameActive) {
            gameResult = "It's a Draw"
        }

        updateGameUI(winningLine)
    }

    private fun updateGameUI(winningLine: IntArray?) {
        if (gameResult != null) {
            Toast.makeText(this, gameResult, Toast.LENGTH_SHORT).show()
            playTheGame.visibility = View.GONE
            score.visibility = View.VISIBLE
            score.text = gameResult
            restart.visibility = View.VISIBLE

            // Обновил изображения только в выигравшей линии
            if (winningLine != null) {
                val gridLayout = findViewById<GridLayout>(R.id.gridlayout)
                for (index in winningLine) {
                    val imageView = gridLayout.getChildAt(index) as ImageView
                    // Установил изображение с перечеркнутым крестиком или ноликом в зависимости от активного игрока
                    if (activePlayer == 0) {
                        imageView.setImageResource(R.drawable.tictactoe_o_strike)
                    } else {
                        imageView.setImageResource(R.drawable.tictcx_strike)
                    }
                }
            }
        } else {
            playTheGame.text = "$currentPlayerName's turn"
        }
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadAd()
                    updateGameUI(null)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is dismissed.
                }
            }
            interstitialAd?.show(this)
        } else {
            startGame()
        }
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    adIsLoading = false
                    val error =
                        "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    adIsLoading = false
                }
            }
        )
    }

    private fun startGame() {
        if (!adIsLoading && interstitialAd == null) {
            adIsLoading = true
            loadAd()
        }
    }

    override fun onDestroy() {
        // Освобождение ресурсов медиаплееров при завершении активности
        mediaPlayer1?.release()
        mediaPlayer2?.release()
        super.onDestroy()
    }
}