package com.project.harbor;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Pair;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * For Game ground 1
 */
class GameModel extends ViewModel {

    class Boat {
        private PointF prec;
        private PointF cur;
        private int col;
        private LinkedList<PointF> path; // path to follow
        private boolean isOn; // is the boat on the sea?

        Boat(PointF prec, PointF cur, int col) {
            this.prec = prec;
            this.cur = cur;
            this.col = col;
            this.path = new LinkedList<>();
            isOn = true;
        }

        PointF getPrec() { return prec; }

        void setPrec(PointF p) { this.prec.set(p); }

        PointF getCur() { return cur; }

        void setCur(float x, float y) { this.cur.set(x, y); }

        void setCur(PointF p) { this.cur.set(p); }

        int getCol() { return col; }

        PointF getNextPoint() { return path.poll(); }
        void addPath(PointF p) { path.add(p); }

        boolean isPath() { return !path.isEmpty();}

        boolean isOn() { return isOn; }

        void setOn() { isOn = false; }
    }

    private int[] harborsCoord; // list of (radiusX, radiusY, radius) of the harbors
    private int[] quaysColors; // quays colors
    private RectF[] quays; // coordinates of the quays
    private Boat[] boats; // all boats of this level
    private int nbToPark, nbParked, nbCurrent, curColor; // number of boats to park, number parked, number sent on the sea
                                                        // and color of the boat to send on the sea
    private int nbOn; // number of on the sea at the same time
    private final int boatHeight, boatWidth;
    private int viewWidth, viewHeight;

    GameModel(int vw, int vh, int nbpk) {
        viewWidth = vw;
        viewHeight = vh;
        harborsCoord = new int[3];
        harborsCoord[0] = viewWidth/2; // radiusX
        harborsCoord[1] = viewHeight/2; // radiusY
        harborsCoord[2] = viewHeight/4; // radius
        quaysColors = new int[2];
        quaysColors[0] = Color.BLUE;
        quaysColors[1] = Color.RED;
        quays = new RectF[2];
        quays[0] = new RectF(harborsCoord[0] - harborsCoord[2],
                harborsCoord[1] - harborsCoord[2]/4,
                harborsCoord[0] - harborsCoord[2]/2,
                harborsCoord[1] + harborsCoord[2]/4);
        quays[1] = new RectF(harborsCoord[0] + harborsCoord[2]/2,
                harborsCoord[1] - harborsCoord[2]/4,
                harborsCoord[0] + harborsCoord[2],
                harborsCoord[1] + harborsCoord[2]/4);
        boatWidth = (int)(quays[0].width()*4/5);
        boatHeight = (int)(quays[0].height()*4/5);
        nbToPark = nbpk;
        nbParked = curColor = 0;
        nbCurrent = nbOn = 0;
        boats = new Boat[nbToPark];
    }


    /**
     * Give coordinate and color of the new boat to send on the sea
     * @param side : the side of the screen where to launch the boat
     * @return [x, y, color]
     */
    Boat newBoat(int side) {
        final int UP_SIDE = 1, LEFT_SIDE = 0, DOWN_SIDE = 3;
        final float SPEED = 5;
        Boat res;
        float x, y;
        if(side == UP_SIDE) {
            x = (float) (Math.random() * (viewWidth - boatWidth ));
            res = new Boat(new PointF(x, -SPEED), new PointF(x, 0), quaysColors[curColor]);
            boats[nbCurrent++]= res;
            nbOn++;
            curColor = (curColor+1)%quaysColors.length;
            return res;
        }
        else if(side == DOWN_SIDE) {
            x = (float) (Math.random() * (viewWidth - boatWidth ));
            res = new Boat(new PointF(x, viewHeight-boatHeight-SPEED),
                    new PointF(x, viewHeight-boatHeight), quaysColors[curColor]);
            boats[nbCurrent++]= res;
            nbOn++;
            curColor = (curColor+1)%quaysColors.length;
            return res;
        }
        else if(side == LEFT_SIDE) {
            y = (float) (Math.random() * (viewHeight - boatHeight));
            res = new Boat(new PointF(-SPEED, y), new PointF(0, y), quaysColors[curColor]);
            boats[nbCurrent++]= res;
            nbOn++;
            curColor = (curColor+1)%quaysColors.length;
            return res;
        }
        else { // (side == RIGHT_SIDE)
            y = (float) (Math.random() * (viewHeight - boatHeight));
            res = new Boat(new PointF(viewWidth-boatWidth-SPEED, y),
                    new PointF(viewWidth-boatWidth, y), quaysColors[curColor]);
            boats[nbCurrent++]= res;
            nbOn++;
            curColor = (curColor+1)%quaysColors.length;
            return res;
        }
    }


    /**
     * Move all the boats on the sea
     */
    void moveBoats() {
        for (int i=0; i<nbCurrent; i++) {
            Boat b = boats[i];
            if(b.isOn()) {
                float x = b.getPrec().x, y = b.getPrec().y;
                b.setPrec(b.getCur());

                // user drawn a path for this boat
                if (b.isPath()) {
                    b.setCur(b.getNextPoint());
                } else {
                    b.setCur(modF(b.getCur().x + (b.getCur().x - x), viewWidth),
                            modF(b.getCur().y + (b.getCur().y - y), viewHeight));
                }
            }
        }
    }

    /**
     * Check if some boats are delivered
     * A boat is delivery if all or part are in one quay of his color :
     * - a boat is in quay if the right up and the right bottom points of the boat
     *      are in the rectangle of the quay for a front side parking
     * - if the left up and the left bottom points of the boat
     *      are in the rectangle of the quay for a rear side parking
     * ...
     * @return a list of the id and the side parked of this boats
     */
    Pair<ArrayList<Integer>, ArrayList<Pair<Float, Float>>> delivery() {
        ArrayList<Integer> id = new ArrayList<>();
        ArrayList<Pair<Float, Float>> park = new ArrayList<>();
        Boat b;
        PointF p;
        for(int i=0; i<nbCurrent; i++) {
            b = boats[i];
            if(b.isOn()) {
                p = b.getCur();
                for (int j = 0; j < quays.length; j++) {
                    if(quaysColors[j] == b.getCol()) {
                        // the front of the boat
                        if (  (quays[j].contains(p.x + boatWidth, p.y + boatHeight/(float)15)
                            && quays[j].contains(p.x + boatWidth,
                                p.y + (boatHeight - boatHeight/(float)15)))
                            ||(quays[j].contains(p.x - boatWidth/(float)20,
                                p.y + boatHeight/(float)15)
                            && quays[j].contains(p.x - boatWidth/(float)20,
                                p.y + (boatHeight - boatHeight/(float)15))) // the rear of the boat
                            ||(quays[j].contains(p.x,
                                p.y + (boatHeight - boatHeight/(float)15))
                            && quays[j].contains(p.x + boatWidth,
                                p.y + (boatHeight - boatHeight/(float)15))) // the bottom of the boat
                            ||(quays[j].contains(p.x, p.y - boatHeight/(float)15)
                            && quays[j].contains(p.x + boatWidth, p.y)) // the up of the boat
                        ) {
                            id.add(i);
                            park.add(new Pair<>(quays[j].centerX(), quays[j].centerY()));
                            b.setOn();
                            nbParked++;
                            nbOn--;
                            break;
                        }
                    }
                }
            }
        }
        return new Pair<> (id, park);
    }

    /**
     * @return null if there is no collision,
     * a Pair of boats index in the collision otherwise
     */
    Pair<Integer, Integer> collision() {
        // collision between boats, we use RectF intersection because boats are rectangle
        Boat b1, b2;
        RectF rect1;
        PointF p;
        for (int i=0; i<nbCurrent-1; i++) {
            b1 = boats[i];
            if(b1.isOn()) {
                p = b1.getCur();
                rect1 = new RectF(p.x, p.y, p.x + boatWidth - boatWidth/(float)20,
                        p.y + boatHeight - boatHeight/(float)15);
                for (int j = i + 1; j < nbCurrent; j++) {
                    b2 = boats[j];
                    if(b2.isOn()) {
                        p = b2.getCur();
                        if (rect1.intersects(p.x, p.y, p.x + boatWidth - boatWidth/(float)20,
                                p.y + boatHeight - boatHeight/(float)15 )) {
                            return new Pair<>(i, j);
                        }
                    }
                }
            }
        }
        // collision with harbor
        // check if one of the four points of each side of the rectangle is in the circle equation
        for (int i=0; i<nbCurrent; i++) {
            Boat b = boats[i];
            if(b.isOn()) {
                p = b.getCur();
                if ((p.x - harborsCoord[0]) * (p.x - harborsCoord[0]) +
                    (p.y - harborsCoord[1]) * (p.y - harborsCoord[1])
                    <= harborsCoord[2] * harborsCoord[2]
                || (p.x + (boatWidth - boatWidth/20) - harborsCoord[0]) *
                    (p.x + (boatWidth - boatWidth/20) - harborsCoord[0]) +
                    (p.y + (boatHeight - boatHeight/15) - harborsCoord[1]) *
                    (p.y + (boatHeight - boatHeight/15) - harborsCoord[1])
                    <= harborsCoord[2] * harborsCoord[2]
                || (p.x - harborsCoord[0]) * (p.x - harborsCoord[0]) +
                    (p.y + (boatHeight - boatHeight/15)- harborsCoord[1]) *
                    (p.y + (boatHeight - boatHeight/15) - harborsCoord[1])
                    <= harborsCoord[2] * harborsCoord[2]
                || (p.x + (boatWidth - boatWidth/20) - harborsCoord[0]) *
                    (p.x + (boatWidth - boatWidth/20) - harborsCoord[0]) +
                    (p.y - harborsCoord[1]) * (p.y - harborsCoord[1])
                    <= harborsCoord[2] * harborsCoord[2]
                ) {
                    return new Pair<>(i, -1);
                }
            }
        }

        return null;
    }


    /**
     * Verify if the line drawn by the user begin on a boat
     * @param x : coord x of the beginning of the line
     * @param y : coord y of the beginning of the line
     * @return the index of the boat or -1
     */
    int boatSelected(int x, int y) {
        for(int i=0; i<nbCurrent; i++) {
            Boat b = boats[i];
            if (b.isOn() && x >= b.getCur().x && x < b.getCur().x+boatWidth
                    && y >= b.getCur().y && y < b.getCur().y+boatHeight) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Add path to a boat, the boat may follow this path for his move
     * @param x : coord x
     * @param y : coord y
     * @param ind : ind of the boat
     */
    void addPath(float x, float y, int ind) {
        Boat b = boats[ind];
        // the boat move on
        if(b.isOn() && b.getCur().x <= b.getCur().y)
            b.addPath(new PointF(x - boatWidth, y - boatHeight/(float)2));
        // the boat reverse
        else if (b.isOn()) b.addPath(new PointF(x, y - boatHeight/(float)2));
    }

    /**
     *
     * @return true if all boats are parked
     */
    boolean endgame() {
        return nbParked == nbToPark;
    }

    /**
     * @return true if all boats of the level are sent on the sea
     */
    boolean allIn() { return nbCurrent >= nbToPark;}


    int[] getHarborsCoord() {
        return harborsCoord;
    }

    int[] getQuaysColors() {
        return quaysColors;
    }

    RectF[] getQuays() {
        return quays;
    }

    Boat[] getBoats() {
        return boats;
    }

    int getBoatWidth() {
        return boatWidth;
    }

    int getBoatHeight() {
        return boatHeight;
    }

    int getNbParked() {
        return nbParked;
    }

    int getNbCurrent() {
        return nbCurrent;
    }

    int getNbOn() {
        return nbOn;
    }

    int getMax() {
        // Max number of boats simultaneously on the sea
        return 5;
    }


    /**
     * Compute a % b where a and b are float
     * @param a foat
     * @param b float
     * @return a%b
     */
    private static float modF(float a, float b) {

        return (float)(a - b * Math.floor(a/b));
    }

}