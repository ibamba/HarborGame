package com.project.harbor;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class GameActivity extends Activity {

    /**
     * Draw the ground of the first level, Amsterdam
     */
    private class GroundLevel1 extends View {

        private Paint fillPaint, strokePaint;
        private Path path;
        private boolean draw;
        private int ind;

        public GroundLevel1(Context context) {
            super(context);
            fillPaint = new Paint();
            strokePaint = new Paint();
            path = new Path();
            draw = false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int radiusX, radiusY, radius;

            // canvas color : blue sky for the sea
            canvas.drawRGB(105, 158, 218);

            // island (color : forest green)
            fillPaint.setColor(Color.rgb(1, 109, 18));
            int[] coord = model.getHarborsCoord();
            radiusX = coord[0];
            radiusY = coord[1];
            radius = coord[2];
            canvas.drawCircle(radiusX, radiusY, radius, fillPaint);

            // parks
            int stroke = 20, cornerX = 20, cornerY = 20;

            // parking 0
            // borders
            int[] qc = model.getQuaysColors();
            RectF[] quays = model.getQuays();
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(qc[0]);
            strokePaint.setStrokeWidth(stroke);
            canvas.drawRoundRect(quays[0], cornerX, cornerY, strokePaint);
            // background
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.rgb(76, 132, 197));
            canvas.drawRoundRect(quays[0], cornerX, cornerY, fillPaint);
            // to open the rect
            strokePaint.setColor(Color.rgb(105, 158, 218));
            canvas.drawLine(radiusX - radius, radiusY - radius/4,
                    radiusX - radius, radiusY + radius/4, strokePaint);

            // parking 1
            // borders
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setColor(qc[1]);
            strokePaint.setStrokeWidth(stroke);
            canvas.drawRoundRect(quays[1], cornerX, cornerY, strokePaint);
            // background
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(Color.rgb(76, 132, 197));
            canvas.drawRoundRect(quays[1], cornerX, cornerY, fillPaint);
            // to open the rect
            strokePaint.setColor(Color.rgb(105, 158, 218));
            canvas.drawLine(radiusX + radius, radiusY - radius/4,
                    radiusX + radius, radiusY + radius/4, strokePaint);

            strokePaint.setStrokeWidth(10);
            strokePaint.setColor(Color.BLACK);
            canvas.drawPath(path, strokePaint);
        }

        public boolean onTouchEvent(MotionEvent event) {
            float pointX = event.getX();
            float pointY = event.getY();
            // Checks for the event that occurs
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if((ind = model.boatSelected((int)pointX, (int)pointY)) != -1) {
                        // Starts a new line in the path
                        draw = true;
                        path.moveTo(pointX, pointY);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Draws line between last point and this point
                    if(draw) {
                        path.lineTo(pointX, pointY);
                        model.addPath(pointX, pointY, ind);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    draw = false;
                    path.reset();
                default:
                    return false;
            }

            postInvalidate(); // Indicate view should be redrawn
            return true; // Indicate we've consumed the touch
        }

    }

    private User user;
    private int level, totalLevel;
    private int nbToPark, current;
    private String levelName;
    private double time;

    private ImageView[] boats; // Image of boats
    private TextView textScore, textTimer;
    private GameModel model;
    private Handler timerHandler, launchHandler, moveHandler; // first for timer, second to launch boats, third to move boats
    private Handler parkHandler, remHandler, endHandler; // first for park boats, second for remove boats, third for end the game
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = (User) (getIntent().getSerializableExtra("USER"));
        String []l = (getIntent().getStringExtra("LEVEL")).split(",");
        levelName = l[0];
        level = Integer.parseInt(l[1]);
        totalLevel = Integer.parseInt(l[2]);
        nbToPark = Integer.parseInt(l[3]);

        current = 0;
        time = 0;

        Point screenSize=new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        model = new GameModel(screenSize.x, screenSize.y, nbToPark);

        setContentView(new GroundLevel1(this));

        // board
        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        textScore = new TextView(this);
        textScore.setText("Parked : 0 / "+nbToPark);
        textScore.setTextAppearance(R.style.board);
        textScore.setBackgroundColor(Color.WHITE);
        textTimer = new TextView(this);
        textTimer.setText("Time (sec) : 0");
        textTimer.setTextAppearance(R.style.board);
        textTimer.setTextColor(Color.RED);
        textTimer.setBackgroundColor(Color.WHITE);
        board.addView(textScore);
        board.addView(textTimer);
        addContentView(board, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        boats = new ImageView[nbToPark];

        // play game background music
        startService(new Intent(getApplicationContext(), GameMusicService.class));

        timerHandler =new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message inputMessage){
                textTimer.setText("Time (sec) : "+time);
            }
        };


        launchHandler =new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message inputMessage){
                launchBoat(model.newBoat((int)(Math.random() * 4)));
            }
        };

        moveHandler =new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message inputMessage){
                GameModel.Boat bm;
                for (int i=0; i<model.getNbCurrent(); i++) {
                    bm = model.getBoats()[i];
                    if(bm.isOn()) {
                        boats[i].setX(bm.getCur().x);
                        boats[i].setY(bm.getCur().y);
                    }
                }
            }
        };

        parkHandler =new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message inputMessage){
                ArrayList<Integer> idx = inputMessage.getData().getIntegerArrayList("IND");
                ArrayList<Pair<Float, Float>> p = new ArrayList<>();
                for(int i = 0; i < (idx != null ? idx.size() : 0); i++) {
                    p.add(new Pair<>(inputMessage.getData().getFloat("centerX"+i),
                            inputMessage.getData().getFloat("centerY"+i)));
                }
                for(int i = 0; i < (idx != null ? idx.size() : 0); i++) {
                    // park boats
                    boats[idx.get(i)].setX(p.get(i).first - model.getBoatWidth()/2);
                    boats[idx.get(i)].setY(p.get(i).second - model.getBoatHeight()/2);
                }

                // update score board
                textScore.setText("Parked : "+model.getNbParked()+" / "+nbToPark);

            }
        };

        remHandler =new Handler(Looper.myLooper()) {
            @Override public void handleMessage(Message inputMessage) {
                // remove boats from the view
                ArrayList<Integer> idx = inputMessage.getData().getIntegerArrayList("IND");
                int ind;
                for(int i=0; i < (idx != null ? idx.size() : 0); i++) {
                    ind = idx.get(i);
                    boats[ind].setVisibility(View.INVISIBLE);
                }
            }
        };

        endHandler =new Handler(Looper.getMainLooper()){
            @Override public void handleMessage(Message inputMessage){
                if(inputMessage.getData().getInt("END") == 1)
                    win();
                else
                    gameover(inputMessage.getData().getInt("B1"),
                            inputMessage.getData().getInt("B2"));
            }
        };

        new GameThread().start();
    }

    /**
     * Notify the UIThread to update timer
     */
    private void notifyUIForTime(){
        Message completeMessage =  timerHandler.obtainMessage();
        completeMessage.sendToTarget();
    }


    /**
     * Notify the UIThread to make a new boat
     */
    private void notifyUIForNew(){
        Message completeMessage =  launchHandler.obtainMessage();
        completeMessage.sendToTarget();
    }

    /**
     * Notify the UIThread to move boats
     */
    private void notifyUIForMove(){
        Message completeMessage =  moveHandler.obtainMessage();
        completeMessage.sendToTarget();
    }

    /**
     * Notify the UIThread to park boats
     * @param d : (list of index of parked boats, list of side to park)
     */
    private void notifyUIForPark(Pair<ArrayList<Integer>, ArrayList<Pair<Float, Float>>>d){
        Message completeMessage =  parkHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList("IND", d.first);
        // first and second have the same size
        // Normally, the size of second doesn't exceed 2 (few probable more than 2 boats will park at the same time)
        for (int i=0; i<d.second.size(); i++) {
            bundle.putFloat("centerX"+i, d.second.get(i).first);
            bundle.putFloat("centerY"+i, d.second.get(i).second);
        }
        completeMessage.setData(bundle);
        completeMessage.sendToTarget();
    }

    /**
     * Notify the UIThread to remove boats
     * @param id : list of index of parked boats to remove
     */
    private void notifyUIForRemove(ArrayList<Integer> id){
        Message completeMessage =  remHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList("IND", id);
        completeMessage.setData(bundle);
        completeMessage.sendToTarget();
    }


    /**
     * Notify the UIThread to end game
     * @param s : 0 for lose, 1 for win
     * @param b1 : the boat 1 in collision in the case s=0
     * @param b2 : the boat 2 in collision in the case s=0
     * */
    private void notifyUIForEnd(int s, int b1, int b2){
        Message completeMessage =  endHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if(s == 0) {
            bundle.putInt("B1", b1);
            bundle.putInt("B2", b2);
        }
        bundle.putInt("END", s);
        completeMessage.setData(bundle);
        completeMessage.sendToTarget();
    }


    /**
     * Class to deal with boat manipulations
     */
    class GameThread extends Thread {

        /**
         * Class to park boats
         */
        private class ParkBoat extends Thread {

            ArrayList<Integer> index;

            ParkBoat(ArrayList<Integer> ind) {
                index = ind;
            }

            @Override public void run() {
                // wait few time
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    Log.e("THREAD", "Sleep failed " + e);
                }
                notifyUIForRemove(index);
            }
        }

        @Override public void run(){
            timer = new Timer();

            // Chrono for time
            final TimerTask t = new TimerTask() {
                /**
                 * To launch boats
                 */
                @Override
                public void run() {
                    time++;
                    notifyUIForTime();
                }
            };


            // launch boats
            TimerTask nw = new TimerTask() {
                /**
                 * To launch boats
                 */
                @Override
                public void run() {
                    if(!model.allIn() && model.getNbOn() < model.getMax()) {
                        notifyUIForNew();
                    }
                }
            };

            // move and deal with game events
            TimerTask move = new TimerTask() {
                /**
                 * To move boats
                 */
                @Override
                public void run() {
                    // move boats
                    model.moveBoats();
                    notifyUIForMove();
                    // checking for delivered
                    Pair<ArrayList<Integer>, ArrayList<Pair<Float, Float>>> d = model.delivery();
                    if(!d.first.isEmpty()) {
                        // park boats
                        notifyUIForPark(d);
                        // remove boats parked
                        new ParkBoat(d.first).start();
                        if(model.endgame()) {
                            // game is end
                            notifyUIForEnd(1, 0, 0);
                            cancel();
                            timer.cancel();
                            timer.purge();
                        }
                    }
                    // checking for collision
                    Pair<Integer, Integer> c = model.collision();
                    if(c != null) {
                        // game is end
                        notifyUIForEnd(0, c.first, c.second);
                        cancel();
                        timer.cancel();
                        timer.purge();
                    }
                }
            };
            timer.scheduleAtFixedRate(t,0,1000); // timer every second
            timer.scheduleAtFixedRate(nw,100,5000); // new boat every 5000 ms
            timer.scheduleAtFixedRate(move,500,50); // move boats every 50 ms
        }
    }

    /**
     * Add a new boat on the sea
     * @param param : boat param
     */
    public void launchBoat(GameModel.Boat param) {
        ImageView b = new ImageView(this);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.boat);
        b.setBackgroundResource(0);
        b.setImageBitmap(bitmap);
        addContentView(b, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        b.getLayoutParams().height = model.getBoatHeight();
        b.getLayoutParams().width = model.getBoatWidth();
        b.setColorFilter(param.getCol());
        b.setX(param.getCur().x);
        b.setY(param.getCur().y);

        boats[current++] = b;
    }

    /**
     * Win dialog
     */
    public void win() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.info);
        alert.setMessage(R.string.win);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.to_continue,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        endgame();
                    }
                }).show();
    }

    /**
     * Game over dialog
     */
    public void gameover(int b1, int b2) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.info);
        String msg;
        if(b2 != -1) msg = getString(R.string.lose_boat)+" "+b1+" & "+b2+"\n\n";
        else msg = getString(R.string.lose_harbor)+" "+b1+"\n\n";
        alert.setMessage(msg);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.to_continue,

                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        endgame();
                    }
                }).show();

    }

    /**
     * Game is finished
     */
    public void endgame() {
        final AlertDialog.Builder a = new AlertDialog.Builder(GameActivity.this);

        // update max score and stat
        // score : (number parked / time) * 1000
        int score = (int)((model.getNbParked() / time) * 1000);
        //username,date,level-name,numberParked,time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");
        final String stat = user.getPseudo()+","+dateFormat.format(new Date())+","+levelName+","
                +score+","+time;

        user.setMaxScore(Math.max(user.getMaxScore(), score));
        a.setTitle(R.string.info);
        a.setMessage(getString(R.string.level_name) + " : " + levelName + "\n" +
                getString(R.string.level_diff) + " : " + level+"/"+totalLevel+"\n" +
                getString(R.string.level_nb_park) + " : " + nbToPark + "\n" +
                getString(R.string.level_nb_parked) + " : "+ model.getNbParked()+"\n"+
                getString(R.string.score) + " : "+ score +"\n"+
                getString(R.string.best_score) + " : "+ user.getMaxScore());
        a.setIcon(android.R.drawable.ic_dialog_info);
        a.setCancelable(false);

        a.setPositiveButton(R.string.to_continue,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();

                        // update infos in MainActivity
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("USER", user);
                        returnIntent.putExtra("STAT", stat);
                        GameActivity.this.setResult(RESULT_OK, returnIntent);
                        finish();
                    }
                }).show();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // stop background music
        stopService(new Intent(getApplicationContext(), GameMusicService.class));
        startService(new Intent(getApplicationContext(), BackgroundMusicService.class));
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(GameActivity.this);
        alert.setTitle(R.string.warning);
        alert.setMessage(R.string.to_leave);
        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        timer.cancel();
                        timer.purge();
                        endgame();
                    }
                });
        alert.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }
}