package src.exampleFuncPlayer;

import src.library.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	@Override
	public Action turn() {
		turn++;

		if (me.unit == SPECS.CASTLE) {
			if (turn == 1) {
				log("Building a pilgrim.");
				return buildUnit(SPECS.PILGRIM, 1, 0);
			}
		}

		if (me.unit == SPECS.PILGRIM) {
			if (turn == 1) {
				// log(Integer.toString([0][getVisibleRobots()[0].castle_talk]));
			}
		}
		return null;
	}
}