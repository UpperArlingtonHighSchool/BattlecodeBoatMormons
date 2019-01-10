package bc19;

import bc19.*;

public class MyRobot extends BCAbstractRobot {

	@Override
	public Action turn() {
		switch (me.unit) {
		case 0: // castle
			return castleAction();
		case 1: // church
			break;
		case 2: // pilgrim
			return pilgrimAction();
		case 3: // crusader
			break;
		case 4: // prophet
			break;
		case 5: // preacher
			break;
		}
	}

	private Action castleAction() {
		if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}
		boolean[][] passMap = getPassableMap();
		int[][] robotMap = getVisibleRobotMap();
		int buildX, buildY;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) {
					continue;
				}
				buildX = me.x + dx;
				buildY = me.y + dy;
				if (buildX > -1 && buildX < passMap[0].length && buildY > -1 && buildY < passMap.length
						&& passMap[buildY][buildX] && robotMap[buildY][buildX] == 0) {
					return buildUnit(SPECS.PILGRIM, dx, dy);
				}
			}
		}
		return null;
	}

	private Action pilgrimAction() {
		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY) {
			int[][] robotMap = getVisibleRobotMap();
			for (int dx = -1; dx >= 1; dx++) {
				for (int dy = -1; dy >=1; dy++) {
					if (getRobot(robotMap[me.y+dy][me.x+dx]).unit == SPECS.CASTLE) {
						return give(dx, dy, me.karbonite, 0);
					}
				}
			}
			return move((int) (Math.random() * 3) - 1, (int) (Math.random() * 3) - 1);
		}

		boolean[][] karbMap = getKarboniteMap();
		if (karbMap[me.y][me.x]) {
			return mine();
		}
		return move((int) (Math.random() * 3) - 1, (int) (Math.random() * 3) - 1);
	}
}
