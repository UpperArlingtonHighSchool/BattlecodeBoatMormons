<<<<<<< HEAD
package exampleFuncPlayer;

import library.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

    public Action turn() {
    	turn++;

    	if (me.unit == SPECS.CASTLE) {
    		if (turn == 1) {
    			log("Building a pilgrim.");
    			return buildUnit(SPECS.PILGRIM,1,0);
    		}
    	}
   
    	if (me.unit == SPECS.PILGRIM) {
    		if (turn == 1) {
    			log("I am a pilgrim.");
=======
package bc19;

//import library.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

    public Action turn() {
    	turn++;

    	if (me.unit == SPECS.CASTLE) {
    		if (turn == 1) {
    			log("Building a pilgrim.");
    			return buildUnit(SPECS.PILGRIM,1,0);
    		}
    	}
   
    	if (me.unit == SPECS.PILGRIM) {
    		if (turn == 1) {
>>>>>>> branch 'master' of https://github.com/UpperArlingtonHighSchool/BoatMormons.git
                 
                //log(Integer.toString([0][getVisibleRobots()[0].castle_talk]));
    		}
    	}

    	return null;

	}
}