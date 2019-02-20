package hochschule_trier.de.pong2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

// Programmierung verteilter und mobiler Anwendungen
// Prof. Dr. Carsten Vogt
// TH Köln, Institut für Nachrichtentechnik
// Stand: 12.02.2016
// Angepasst durch G.Rock für das Praesenzpraktikum


// View mit einer Animation, in der sich ein Bild (jetzt der Ball) über das Display bewegt
// Implementierung des SurfaceHolder.Call Interfaces zur Verarbeitung
// von Rückmeldungen des SurfaceViews an den Thread
// Siehe Folien 04 ab Seite 111
@SuppressLint("WrongCall")
class Pong2View extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {

    private static final float TEXTSIZE = 50;// TODO: einstellungen, Paint jede Buchstabe
    private static final float SPEEDUP = 1.2f;
    private static final int SHRINKAGE = 5;
    private static final int MINRADIUS = 3;
    private static final int INITRADIUS = 100;
    private static final float PITCHFACTOR = 2;
    private final float paddleXpos = 10; //Position aendert sich nicht. x-Koordinate der oberen linken Ecke des Rechteck
    private Paint paintText = new Paint();
    private int textColor = 0xFFFFFF00;
    private Context context; // Activity, die dem View zugeordnet ist
    private int score;
    private int highscores[];
    // private Bitmap bitmap;   // zu bewegendes Bild
    private Paint paintBall = new Paint();
    private Paint paintPaddle = new Paint();
    private Paint paintBG = new Paint();
    private float xPos;      // aktuelle X-Position des Balles
    private float yPos;      // aktuelle Y-Position des Balles
    private float xDirect;   // aktuelle X-Richtung des Balles
    private float yDirect;   // aktuelle Y-Richtung des Balles
    private int ballRadius = INITRADIUS;
    private int ballColor = 0xFFFF0000;
    private float paddleYpos; // y-Koordinate der oberen linken Ecke des Rechteck
    private float paddleWidth = 10;
    private int paddleHeight = 200;
    private int paddleColor = 0xFFFFFFFF;

    private int backgroundColor = 0xFF000000;

    //Wird auch hier benötigt zur Realisierung der Animation
    AnimationThread animThread = null;  // Thread, der die Animation steuert

    //private GestureDetector exampleGestureDetector;  // GestureDetector, über den das Bild neu angestoßen werden kann

    // Wird nur dann benötigt, wenn man das Paddle zur Animationszeit verändern will
    // Wird in dieser Version nicht realisiert.
    //private ScaleGestureDetector scaleGestureDetector;

    // Konstruktor
    public Pong2View(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);
        // Initialisierung der Attribute
        this.context = context;
        //this.xPos = xPos; // Wird in surfaceCreated() gesetzt.
        initGame();

        // Erzeugung einer Bitmap für das darzustellende Bild
        // bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        // bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        paintBall.setColor(ballColor);
        paintPaddle.setColor(paddleColor);
        paintBG.setColor(backgroundColor);
        paintText.setColor(textColor);
        paintText.setTextSize(TEXTSIZE);

        // Erzeugung des GestureDetectors: Nicht in dieser Version.
        //exampleGestureDetector = new GestureDetector(getContext(),new ExampleGestureListener());

        // Registrierung für Callback-Methoden des Holders, der diesem SurfaceView zugeordnet ist.
        // Bewirkt, dass die Methode surfaceCreated() (siehe unten) aufgerufen wird, sobald der SurfaceView bereit ist.
        getHolder().addCallback(this); //implements SurfaceHolder.Callback
    }

    private void initGame() {
        this.xPos = getWidth() - ballRadius;
        this.yPos = zufAnfangsPosY();
        this.xDirect = -10;
        this.yDirect = zufAnfangsDirectY();
        this.score = 0;
        ballRadius = INITRADIUS;
    }

    // onDraw() zeichnet das Bild an seiner aktuellen Position
    // Aufruf wenn die Oberfläche neu gezeichnet wird
    // Wir müssen das zuvor vorhandene Bild überzeichnen, den Ball ergänzen, die Hintergrundfarbe,
    // die Position und Farbe des Balles (Kreis), Farbe des Paddles,
    // Paddles selbst als Rechteck, Text schreiben für den bisherigen Highscore
    // -->
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // löschen des vorherigen Hintergrundes, indem alle Bildpunkte auf die BackgroundColor gesetzt werden
        // -->
        canvas.drawColor(backgroundColor);

        // Zeichnen des Balls an der Position xPos, yPos mit Radius ballRadius
        // -->
        //canvas.drawBitmap(bitmap, xPos, yPos, null);
        canvas.drawOval(xPos - ballRadius, yPos - ballRadius, xPos + ballRadius, yPos + ballRadius, paintBall);

        // Zeichnen des Paddles mit der paddleColor an der Position paddleXpos, paddleYpos
        // mit der Breite paddleWith und der Höhe paddleHight
        // Methode heisst drawRect( , , , , )
        // -->
        //canvas.drawRect(xPos,paddleYpos,paddleWidth,paddleHeight,bitmap);
        canvas.drawRect(paddleXpos,paddleYpos,paddleXpos+paddleWidth,paddleYpos+paddleHeight,paintPaddle);

        // Zeichnen des Textes mit der Farbe des Balles (ballColor) an der Position 200, 40
        // Textinhalt: Score, Highscore0, Highscore1 und Highscore2;
        // Setzen Sie mit Hilfe des Paint Objektes die Textgröße passend.
        // -->
        int textX = 40;
        int textY = (int) paintText.getTextSize();
        String text = "Score: " + score + ", Highs:" + highscores[0] + ", " + highscores[1] + ", " + highscores[2];
        canvas.drawText(text,textX, textY, paintText);

    }

    // Die folgenden drei Methoden sind erforderlich, um das Interface SurfaceHolder.Callback zu implementieren.
    // Wichtig ist davon hier surfaceCreated(): Die Methode wird automatisch aufgerufen,
    // sobald der SurfaceView bereit für das Zeichnen ist, und startet dann den Animations-Thread.
    // -->
    public void surfaceCreated(SurfaceHolder holder) {
        // Tu nichts, wenn der Animations-Thread schon existiert.
        // -->
        if (animThread != null) {
            return;
        }

        // Bestimmung der xPos des Balles am rechten Rand des Surface Views
        // Tipp: getWidth liefert die Breite des aktuellen Views.
        // -->
        initGame();

        // Bestimmung der paddleYpos.
        // Tip: Beispiel: die Hälfte der Differenz der View Höhe und der Paddle Höhe
        // -->


        // Erzeugen und Starten des Animation Thread, wenn der Surface View gebaut ist
        // Thread bekommt den Holder als Argument, damit er auf das SurfaceView zugreifen kann
        // -->
        animThread = new AnimationThread(holder);
        animThread.start();
    }

    // Bleibt unverändert, da sich unser SurfaceView nicht ändert
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    // Bleibt unverändert
    public void surfaceDestroyed(SurfaceHolder holder) {
        animThread.stop = true;      // Animations-Thread anhalten, wenn der SurfaceView nicht mehr angezeigt wird
    }

    //liefert eine zufällige Anfangsposition für den Ball für die Y Koordinate
    // -->
    float zufAnfangsPosY() {
        float minY = ballRadius;
        float maxY = getHeight() - ballRadius;
        return (float) (minY + Math.random() * (maxY - minY));
    }

    // liefert Zahl zwischen -5 und 4.99
    // -->
    float zufAnfangsDirectY() {
        return (float) (-5 + Math.random() * 10);
    }


    // onTouchEvent() wird automatisch aufgerufen, wenn auf dem View ein Berührungsereignis bemerkt wird:
    // Der aktuelle Animations-Thread soll nicht angehalten werden.
    // Der GestureDetector wird in dieser Version nicht benötigt.
    // Es soll lediglich anhand des MotionEvents (siehe Foliensatz 04, Seite 85) und der Art
    // des Ereignisses die Bewegung des Spielers erkannt werden und das Paddle entsprechend
    // bewegt werden (setzen der zugehörigen Paddle Koordinaten).
    public boolean onTouchEvent(MotionEvent ev) {

        // Initialisierung der Variablen zur Verwaltung der Position des Paddle und des Pointers
        float firstTouchY = 0;
        float startPosPaddle = 0;

        //animThread.stop = true;
        //exampleGestureDetector.onTouchEvent(ev); // benachrichtigt den GestureDetector => neue Richtung des Icons


        // Implementieren Sie eine Fallunterscheidung über die Art des Ereignisses. Benutzen Sie hierzu
        // getAction() und die passende Aktionsmaske, die Ihnen das Ergebnis von getAction passend filtert
        // und nur die Art des Ereignisses enthält.
        // -->
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {

            // case: Geste hat begonnen.
            //       Setzen der Anfangswerte des Paddle und des Pointers/Touches
            //       in der y-Koordinate.
            // -->
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                movePaddleTo(ev.getY() - paddleHeight/2);
                break;
            }

            // case: Geste wird fortgeführt.
            //       Das Paddle soll an die Stelle des Pointers springen (y-Koordinate des Paddles),
            //       und um die Differenz der y-Koordinaten der durch den Nutzer ausgeführten Bewegung
            //       bewegt werden. Achten Sie dabei auch auf die Richtung der Bewegung.
            //       Beachten Sie die Sonderfälle, dass das Paddle oben oder unten an den Rand stößt.

            //       Spielart: Paddle entgegen der Richtung der Bewegung  bewegen :-
                // TODO: move erzeugt Geschwindigkeit, up erzeugt Driften...
            // case: Geste ist beendet. Brauchen wir diesen Fall?
            //
            case MotionEvent.ACTION_UP: {

                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                break;
            }
        }

        return true;
    }

    private void movePaddleTo(float v) {
        paddleYpos = v;
        if(paddleYpos<0){
            paddleYpos = 0;
        }
        if(paddleYpos>getHeight() - paddleHeight){
            paddleYpos = getHeight() - paddleHeight;
        }
    }


    // Alle Methoden zur Individualisierung und Highscore Getter/Setter
    // sollen im Folgenden programmiert werden. Die Signatur ist jeweils vorgegeben und
    // größtenteils auch der Code.
    //-->


    // Getter für das Highscores Feld
    // -->
    int[] getHighscores() {
        return (int[]) highscores; //.clone();
    }

    // Setter für das Highscores Feld
    // -->
    void setHighscores(int[] highscores) {

        this.highscores = (int[]) highscores; //.clone();
    }


    // Setzen des neuew Highscores und anpassen der bisherigen Highscores.
    // -->
    void addHighscore(int newScore) {
        if (newScore <= highscores[2]) return;
        if (newScore > highscores[2] && newScore <= highscores[1]) highscores[2] = newScore;
        if (newScore > highscores[1] && newScore <= highscores[0]) {
            highscores[2] = highscores[1];
            highscores[1] = newScore;
        }
        if (newScore > highscores[0]) {
            highscores[2] = highscores[1];
            highscores[1] = highscores[0];
            highscores[0] = newScore;
        }
    }

    // Setzen des Ball Radius
    // -->
    void setBallRadius(int ballRadius) {
        this.ballRadius = ballRadius;
    }

    // Setzen der Ball Farbe
    // -->
    void setBallColor(int ballColor) {
        this.ballColor = ballColor;
    }

    // Setzen der Paddle Höhe
    // -->
    void setPaddleHeight(int paddleHight) {
        this.paddleHeight = paddleHeight;
    }

    // Setzen der Paddle Farbe
    // -->
    void setPaddleColor(int paddleColor) {
        this.paddleColor = paddleColor;
    }

    // Setzen der Hintergrundfarbe
    // -->
    void setBackgrdColor(int backgrdColor) {
        this.backgroundColor = backgrdColor;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float pitch = event.values[1];
        movePaddleTo(paddleYpos + pitch * PITCHFACTOR);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // Thread, der die Animation ausführt, also das Bild bewegt
    // In diesem Fall wird der Ball bewegt
    private class AnimationThread extends Thread {

        // Bleibt unverändert
        boolean stop = false;        // gibt an, ob der Thread weiterlaufen soll
        SurfaceHolder surfaceHolder; // zum Zugriff auf den SurfaceView
        Canvas c;                    // Canvas, auf den gezeichnet werden soll

        // Konstruktor des Animations-Threads
        // Stellt die Verbindung zwischen Thread und Surface her (über den surfaceHolder)
        // Siehe Erzeugung des Thread in der surfaceCreated()-Methode
        // -->
        public AnimationThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        // Ausführungsschritte des Threads. Implementierung der run()-Methode
        // -->
        public void run() {
            // Endlosshleife zum Bewegen des Bilds
            // -->
            while (!stop) {
                // Veränderung der x- und y-Position anhand des aktuellen Richtungsvektors
                // -->
                xPos += xDirect;
                yPos += yDirect;


                // Paddle getroffen:
                // Überlegen Sie sich was gilt bzgl. der x-Achse und der y-Achse
                // Bedingungen und Berechnung der Veränderung der Ballrichtung
                // Der Ball muss sozusagen am rechten Rand des Paddles anschlagen. Man muss dies
                // nicht direkt bemerken. Es reicht, wenn man eine unscharfe Bedingung erfüllt.
                // Zusätzlich muss der Ball auch in der Höhe des Paddle treffen, also in der y-Achse.
                // Beachten Sie ebenfalls den Radius des Balls in Ihren Berechnungen
                // Einfallswinkel = Ausfallwinkel
                // Erhöhung des Scores
                // Setzen der x-Position des Balls
                // -->
                if (((xPos - ballRadius) < (paddleXpos + paddleWidth))
                        && (yPos > paddleYpos)
                        && (yPos < paddleYpos + paddleHeight)) {
                    xDirect = -xDirect;
                    xPos = paddleXpos + paddleWidth + ballRadius;
                    score++;
                    yDirect *= SPEEDUP;
                    xDirect *= SPEEDUP;
                    if (ballRadius - SHRINKAGE > MINRADIUS) {
                        ballRadius -= SHRINKAGE;
                    }
                }


                // Wenn oberer oder unterer oder rechter Rand des Displays erreicht werden
                // Änderung des Richtungsvektors nach dem Prinzip Einfallswinkel = Ausfallswinkel
                // Ball im Aus heisst: Sofort neues Spiel mit Anfangskonfiguration, Hishscore speichern,
                // aktueller Score auf 0 setzen.

                // rechter Rand
                //-->
                if (xPos + ballRadius > getWidth()) {
                    xDirect = -xDirect;
                    xPos = getWidth() - ballRadius;
                }


                // oberer Rand
                // -->
                if (yPos - ballRadius < 0) {
                    yDirect = -yDirect;
                    yPos = 0 + ballRadius;
                }

                // unterer Rand
                //-->
                if (yPos + ballRadius > getHeight()) {
                    yDirect = -yDirect;
                    yPos = getHeight() - ballRadius;
                }


                // Ball im Aus
                // -->
                if (xPos - ballRadius < 0) {
                    xDirect = -xDirect;
                    //TODO: Verschwinden + ggf. highscore schreiben + toast
                    for (int ix = 0; ix < highscores.length; ix++) {
                        if (score > highscores[ix]) {
                            for (int jx = highscores.length - 1; jx > ix; jx -- ) {
                                highscores[jx] = highscores[jx - 1];
                            }
                            highscores[ix] = score;
                            break;
                        }
                    }
                    initGame();
                }


                // Rest bleibt wie bei dem Ursprungsbeispiel unverändert
                c = null;
                try {
                    // Belegung des Canvas
                    c = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        // Zeichnen auf dem Canvas (siehe Definition der Methode onDraw() oben
                        onDraw(c);
                    }
                } catch (Exception e) {
                } finally {
                    if (c != null)
                        // Freigabe des Canvas
                        surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }


    }


    // Wird in dieser Version nicht verwendet!
    /*
    public class ExampleGestureListener extends GestureDetector.SimpleOnGestureListener {
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float xDirect = (e2.getX() - e1.getX()) / 2;
            float yDirect = (e2.getY() - e1.getY()) / 2;
            ((Activity) context).setContentView(new Pong2View(context, null, 0));
            return false;
        }
    }*/


}

public class MainActivity extends Activity {
    Pong2View pong2View; //Surface View für das Spiel

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Erzeugen des Views zur Darstellung des Spiels.
        // View soll erst erscheinen, wenn wir das Spiel über das Optionsmenü starten
        pong2View = new Pong2View(this, null, 0);

        setContentView(R.layout.activity_main);

        // Highscore als int Feld anlegen für die besten drei erzielten scores
        // Auslesen der Scores aus den SharedPreferences, falls schon Werte drin stehen
        //-->
        int[] highscores = new int[]{0, 0, 0};
        SharedPreferences sd = getSharedPreferences("score", 0);
        highscores[0] = sd.getInt("score", 0);


        pong2View.setHighscores(highscores);
    }

    // Optionsmenue mit Showsettings und StartGame
    // Bleibt so drin. Teilnehmer bauen die passende menur_main.xml
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    // Auswahl der ShowSettings startet die Anzeige einer Preferenc-Activity mit der Anzeige
    // aller derzeit gesetzten induviduellen Einstellungen
    // Auswahl von StartGame nimmt aus den SharedPreferences die eingestellten Werte, setzt diese mit
    // mit Hilfe der definierten Hilfsfunktionen, setzt den aktuellen View auf pongView.
    // Eine Hilfsfunktion, die Farben auf Farbcodes mappt ist colorStringToColor (s.u.)
    // -->
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.start_game) {
            startGame(findViewById(R.id.btn_start_game));
            return true;
        }
        if (itemId == R.id.high_scores) {
            int[] high = pong2View.getHighscores();
            // TODO: call a designer
            String highScoreMessage = "";
            for (int s : high) {
                highScoreMessage += s + "\n";
            }
            Toast.makeText(this, highScoreMessage, Toast.LENGTH_LONG).show();
        }


        return super.onOptionsItemSelected(item);
    }

    public void startGame(View view) {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(pong2View, s, SensorManager.SENSOR_DELAY_FASTEST);
        setContentView(pong2View);
    }

    // Hilfsfunktion bleibt so drin
    public int colorStringToColor(String colorString) {
        switch (colorString) {
            case "Weiß":
                return Color.WHITE;
            case "Schwarz":
                return Color.BLACK;
            case "Rot":
                return Color.RED;
            case "Grün":
                return Color.GREEN;
            case "Blau":
                return Color.BLUE;
            default:
                return 0;
        }
    }

    // Bei der Methode onStop müssen die aktuellen Highscores in den SharedPreferences "Highscores"
    // gespeichert werden
    // -->
    @Override
    public void onStop() {
        super.onStop();


    }

}


















