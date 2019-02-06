package derichs.david.elfmeter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

/**
 * @author david derichs 814614
 */

public class MainActivity extends AppCompatActivity {

    private Canvas mCanvas;
    private Paint mPaint = new Paint();
    private Paint mPaintText = new Paint(Paint.UNDERLINE_TEXT_FLAG);
    private Bitmap mBitmap;
    private ImageView mImageView;
    private static final int OFFSET = 120;
    private int mColorBackground;
    private int mColorRectangle;
    private int mColorAccent;
    private boolean fieldDrawn = false;

    private boolean currentPlayerAlreadyShoot = false;

    private long activePlayerID = 1;
    private long player_1_goals = 0;
    private long player_2_goals = 0;

    private DatabaseReference mDatabase;

    /**
     * Class GameStats is used to define GameData used in Firebase-Database
     */
    @IgnoreExtraProperties
    public class GameStats {

        public long current_player_id;
        public long goals_player_1;
        public long goals_player_2;

        public GameStats() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public GameStats(long current_player_id, long goals_player_1, long goals_player_2) {
            this.current_player_id = current_player_id;
            this.goals_player_1 = goals_player_1;
            this.goals_player_2 = goals_player_2;
        }
    }

    /**
     * Takes the giben Parameters and writes them into the Firebase database.
     * @param gameStat_ID Game ID to determine, which game the Player wants to take part of.
     * @param current_player_id Player_id of the player who's turn it is right now.
     * @param goals_player_1 Amount of goals that player_1 has scored so far.
     * @param goals_player_2 Amount of gloals that player_2 has scored so far.
     */
    public void writeGameStats(String gameStat_ID, long current_player_id, long goals_player_1, long goals_player_2){
        Log.d("Elfmeter", "Wrting to Database");
        GameStats stats = new GameStats(current_player_id, goals_player_1, goals_player_2);
        mDatabase.child("gamestats").child(gameStat_ID).setValue(stats);
    }


    /**
     * On Create Method to define, what the app should do if started.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initDatabase();

        this.initColors();
        this.useColors();
        this.initImageView();
        this.observeImageView();

        updateResultText_ActivePlayer();
        updateResultText();


    }

    /**
     * Initializes the Firebase-Database and adds a listener to ValueChanges.
     */
    private void initDatabase(){
        FirebaseApp.initializeApp(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        ValueEventListener gameStatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI

                if(dataSnapshot.getValue() != null){
                    Log.d("Elfmeter", "Data Received: " + dataSnapshot.getValue());
                    activePlayerID = (long) dataSnapshot.child("gamestats").child("spiel").child("current_player_id").getValue();
                    player_1_goals = (long) dataSnapshot.child("gamestats").child("spiel").child("goals_player_1").getValue();
                    player_2_goals = (long) dataSnapshot.child("gamestats").child("spiel").child("goals_player_2").getValue();
                    updateResultText_ActivePlayer();
                    updateResultText();
                } else {
                    Log.d("Elfmeter", "No Data in dataSnapshot");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("Elfmeter", "DatabaseError: ", databaseError.toException());
                // ...
            }
        };
        mDatabase.addValueEventListener(gameStatListener);
    }

    /**
     * Observes the ImageView and checks, if it hast been drawn already.
     * This is important, because other methods rely on getWidth and getHeight to be not 0
     */
    private void observeImageView(){
        final ViewTreeObserver vto = mImageView.getViewTreeObserver();
        if (vto.isAlive()) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                public void onGlobalLayout() {
                    if(fieldDrawn != true) {
                        drawField(mImageView);
                    }
                }
            });
        }
    }

    /**
     * Initialzies all Colors used by elements in this App.
     */
    private void initColors(){
        mColorBackground = ResourcesCompat.getColor(getResources(),
                R.color.colorBackground, null);
        mColorRectangle = ResourcesCompat.getColor(getResources(),
                R.color.colorRectangle, null);
        mColorAccent = ResourcesCompat.getColor(getResources(),
                R.color.colorAccent, null);
    }

    /**
     * Takes the layout-colors and attaches then to the different paint-objects.
     */
    private void useColors(){
        mPaint.setColor(mColorRectangle);

        mPaintText.setColor(
                ResourcesCompat.getColor(getResources(),
                        R.color.colorRectangle, null)
        );
        mPaintText.setTextSize(70);
    }

    /**
     * Initializes the Images-View object, to be used to draw a Canvas on.
     */
    private void initImageView(){
        mImageView = (ImageView) findViewById(R.id.myimageview);
    }

    /**
     * Draws the football field on the ImageView with a given Position of the ball.
     * @param view View to be drawn on.
     * @param BallPosition Position of the ball.
     */
    public void drawField(View view, int BallPosition){
        if(view.getWidth() == 0){
            this.fieldDrawn = false;
            return;
        }

        this.fieldDrawn = true;

        int vWidth = view.getWidth();
        int vHeight = view.getHeight();

        mBitmap = Bitmap.createBitmap(vWidth, vHeight, Bitmap.Config.ARGB_8888);
        mImageView.setImageBitmap(mBitmap);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(mColorBackground);

        this.drawGoal(view, mCanvas);
        this.drawBall(view, mCanvas, BallPosition);
    }

    /**
     * Draws a ball on the giben ImageView/Canvas on the specified position.
     * @param view ImageView
     * @param mCanvas Canvas to be drawn on
     * @param ballPosition Position of the ball. This is an x-Parameter, y is always the same.
     */
    public void drawBall(View view, Canvas mCanvas, int ballPosition){
        int vWidth = view.getWidth();
        int vHeight = view.getHeight();

        int halfWidth = vWidth / 2;
        int halfHeight = vHeight / 2;

        int quartWidth = halfWidth / 2;
        int quartHeight = halfHeight / 2;

        int octHeight = quartHeight / 2;

        mCanvas.drawCircle(ballPosition, octHeight+30, octHeight/4 , mPaint );
    }

    /**
     * Draws the default Field with the ball on it's default position before the shot.
     * @param view ImageView to be drawn on.
     */
    public void drawField(View view){

        if(view.getWidth() == 0){
            this.fieldDrawn = false;
            return;
        }

        this.fieldDrawn = true;

        int vWidth = view.getWidth();
        int vHeight = view.getHeight();

        mBitmap = Bitmap.createBitmap(vWidth, vHeight, Bitmap.Config.ARGB_8888);
        mImageView.setImageBitmap(mBitmap);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawColor(mColorBackground);

        this.drawGoal(view, mCanvas);
        this.drawBall(view, mCanvas);
    }

    /**
     * Draws the ball on the Field at it's default position.
     * @param view ImageView to be drawn on
     * @param mCanvas Canvas inside of the ImageView to be drawn on.
     */
    public void drawBall(View view, Canvas mCanvas){
        int vWidth = view.getWidth();
        int vHeight = view.getHeight();

        int halfWidth = vWidth / 2;
        int halfHeight = vHeight / 2;

        int quartWidth = halfWidth / 2;
        int quartHeight = halfHeight / 2;

        int octHeight = quartHeight / 2;

        mCanvas.drawCircle(halfWidth, vHeight-quartHeight, octHeight/4 , mPaint );

    }

    /**
     * Draws a Goal on the field
     * @param view ImageView to be drawn on.
     * @param mCanvas Canvas inside of the ImageView to be drawn on.
     */
    public void drawGoal(View view, Canvas mCanvas){
        int vWidth = view.getWidth();
        int vHeight = view.getHeight();

        int halfWidth = vWidth / 2;
        int halfHeight = vHeight / 2;

        int quartWidth = halfWidth / 2;
        int quartHeight = halfHeight / 2;

        int octHeight = quartHeight / 2;

        mCanvas.drawLine(halfWidth-quartWidth, octHeight, halfWidth+quartWidth, octHeight, mPaint);
        mCanvas.drawLine(halfWidth-quartWidth, quartHeight, halfWidth+quartWidth, quartHeight, mPaint);

        for(int i = 0; i<octHeight; i+=20){
            mCanvas.drawLine(halfWidth-quartWidth, octHeight+i, halfWidth+quartWidth, octHeight+i, mPaint);

        }
        for(int i = 0; i<halfWidth; i+=20) {
            mCanvas.drawLine(halfWidth - quartWidth + i, octHeight, halfWidth - quartWidth + i, quartHeight, mPaint);
        }

        mCanvas.drawLine(halfWidth-quartWidth, octHeight, halfWidth-quartWidth, quartHeight, mPaint);
        mCanvas.drawLine(halfWidth+quartWidth, octHeight, halfWidth+quartWidth, quartHeight, mPaint);

        mCanvas.drawText("GOAL",halfWidth-100, quartHeight-octHeight/2, mPaintText );
    }

    /**
     * If Button "shoot" is clicked, then this method simulates the new ball position and updates the app.
     * @param view ImageView to be drawn on, if necessary.
     */
    public void shootBall(View view){
        if (currentPlayerAlreadyShoot) return;
        if(this.fieldDrawn != true) return;
        int ballPositionAfterShoot = getRandomBallPosition();
        if(ballInGoal(ballPositionAfterShoot)){
            if(activePlayerID == 1) player_1_goals += 1;
            if(activePlayerID == 2) player_2_goals += 1;
        } else {
            Log.d("Elfmeter", "Daneben");
        }
        drawField(mImageView, ballPositionAfterShoot);
        currentPlayerAlreadyShoot = true;
        writeGameStats("spiel", this.activePlayerID, this.player_1_goals, this.player_2_goals);
    }

    /**
     * Checks, if the ball has landed inside of the goal.
     * @param ballPosition Position of the ball.
     * @return true, if ball has landed inside of goal.
     */
    private boolean ballInGoal(int ballPosition){
        int vWidth = mImageView.getWidth();
        int halfWidth = vWidth / 2;
        int quartWidth = halfWidth / 2;

        int minX = halfWidth-quartWidth;
        int maxX = halfWidth+quartWidth;

        if(ballPosition>minX && ballPosition<maxX) return true;

        return false;
    }

    /**
     * Calculates a new position of the ball, which can be inside or outside the goal.
     * @return int-value, which describes the new x-coordinate of the ball.
     */
    private int getRandomBallPosition(){

        int max = mImageView.getWidth();
        int min = 0;

        Random pos = new Random();
        return pos.nextInt((max - min) + 1) + min;
    }

    /**
     * Method to be called, if button "next" is clicked.
     * @param view Button-View-Element
     */
    public void nextPlayer(View view){
        currentPlayerAlreadyShoot = false;
        changePlayer();
        updateResultText_ActivePlayer();
        drawField(mImageView);
        writeGameStats("spiel", this.activePlayerID, this.player_1_goals, this.player_2_goals);
    }

    /**
     * Changes the current Player to the other.
     */
    private void changePlayer(){
        if(this.activePlayerID == 1) {
            this.activePlayerID = 2;
        } else this.activePlayerID = 1;
    }

    /**
     * Resets all Game-Values to their default values.
     * @param view Button-View Object, which was clicked..
     */
    public void resetGame(View view){
        this.player_1_goals = 0;
        this.player_2_goals = 0;
        drawField(mImageView);
        this.currentPlayerAlreadyShoot = false;
        this.activePlayerID = 1;
        writeGameStats("spiel", this.activePlayerID, this.player_1_goals, this.player_2_goals);
    }

    /**
     * Updates the result_text, which shows, how the scoring points are currently.
     */
    private void updateResultText(){
        TextView result = (TextView) findViewById(R.id.resultText_result);
        result.setText(player_1_goals + " : " + player_2_goals);
    }

    /**
     * Updates the resultText in order to show, who's players turn it is.
     * Active Player is in white text color, the other has color gray.
     */
    private void updateResultText_ActivePlayer(){
        TextView pl1_text = (TextView) findViewById(R.id.resultText_PL1);
        TextView pl2_text = (TextView) findViewById(R.id.resultText_PL2);

        if(activePlayerID == 1) {
            pl1_text.setTextColor(Color.WHITE);
            pl2_text.setTextColor(Color.GRAY);
        }

        if(activePlayerID == 2){
            pl1_text.setTextColor(Color.GRAY);
            pl2_text.setTextColor(Color.WHITE);
        }
    }
}
