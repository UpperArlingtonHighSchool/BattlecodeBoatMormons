package bc19;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
	public static int counter;

	public Action turn() {
		counter++;
		if (me.unit == SPECS.CASTLE) {
			log("Castle " + me.id + " says " + counter);
		}
		return null;
	}
}
