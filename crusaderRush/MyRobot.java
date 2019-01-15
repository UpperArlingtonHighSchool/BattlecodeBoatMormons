package bc19;

import bc19.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class MyRobot extends BCAbstractRobot {
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private final int[] attackPriority = new int[] {4, 5, 3, 0, 1, 2}; // 0: Castle, 1: Church, 2: Pilgrim, 3: Crusader, 4: Prophet,
	private boolean hRefl; // true iff reflected horizontally				 5: Preacher. Feel free to mess with order in your robots.
	private boolean startLowLoc;
	private int[][] robotMap;
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel
	private int numCastles;
	private int[] castleIDs = new int[3]; // small so we don't worry about if there's only 1 or 2 castles
	private int[][] plainCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int[] encodedCastleLocs = new int[3];
	private int[][] enemyCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int encodedLocError; // Only for use by castles in first few turns
	private boolean attack;

	public Action turn() {
		if(me.turn == 1)
		{
			getFMap();
			hRefl = getReflDir();
		}
		robotMap = getVisibleRobotMap();
		switch (me.unit) {
		case 0: return castle();
		case 1: return church();
		case 2: return pilgrim();
		case 3: return crusader();
		case 4: return prophet();
		case 5: return preacher();
		}
		return null;
	}

	private Action castle()
	{
		if(me.turn == 1)
		{
			numCastles = 1;
			castleIDs[0] = me.id;

			for(Robot rob : getVisibleRobots())
			{
				if(rob.team == me.team && rob.id != me.id)
				{
					castleIDs[numCastles] = rob.id;
					numCastles += 1;
				}
			}

			if(numCastles > 1)
			{
				sendOwnLoc();
			}
		}

		else if(me.turn == 2)
		{
			if(numCastles > 1)
			{
				sendOwnLoc();
			}

			for (int i = 1; i < numCastles; i++)
			{
				encodedCastleLocs[i] = getRobot(castleIDs[i]).castle_talk;
				decodeCastleLoc(i);
			}

			sendCastleLocs(1);
		}

		else if(me.turn == 3)
		{
			castleTalk(encodedLocError); // Only 2 bits so feel free to add more info and also quite unimportant overall
		}

		else if(me.turn == 4)
		{
			castleTalk(encodedLocError); // Only 2 bits so feel free to add more info and also quite unimportant overall

			for (int i = 1; i < numCastles; i++)
			{
				fixLocError(getRobot(castleIDs[i]).castle_talk, i);
			}
			getEnemyCastleLocs();
		}

		else if(me.turn == 6)
		{
			sendCastleLocs(5);
			signal(0, 0);
		}

		if(me.turn > 1 && me.turn < 7)
		{
			if(hRefl)
			{
				if(me.x < fullMap.length / 2)
				{
					return buildUnit(SPECS.CRUSADER, 1, me.turn % 3 - 1);
				}
				else
				{
					return buildUnit(SPECS.CRUSADER, -1, me.turn % 3 - 1);
				}
			}
			else
			{
				if(me.y < fullMap.length / 2)
				{
					return buildUnit(SPECS.CRUSADER, me.turn % 3 - 1, 1);
				}
				else
				{
					return buildUnit(SPECS.CRUSADER, me.turn % 3 - 1, -1);
				}
			}
		}

		return null;
	}


	private Action church()
	{
		return null;
	}


	private Action pilgrim()
	{
		return null;
	}


	private Action crusader()
	{
		if(me.turn == 1)
		{
			attack = false;

			for(Robot rob : getVisibleRobots())
			{
				if(rob.unit == SPECS.CASTLE)
				{
					castleIDs[0] = rob.id;
				}
			}

			if(hRefl)
			{
				if(me.x < fullMap.length / 2)
				{
					startLowLoc = true;
					return move(1, 0);
				}
				else
				{
					startLowLoc = false;
					return move(-1, 0);
				}
			}
			else
			{
				if(me.y < fullMap.length / 2)
				{
					startLowLoc = true;
					return move(0, 1);
				}
				else
				{
					startLowLoc = false;
					return move(0, -1);
				}
			}
		}

		if(!attack)
		{
			if(getVisibleRobots().length == 6)
			{
				getAllCastleLocs();
				getEnemyCastleLocs();

				attack = true;
			}
		}

		if(attack)
		{
			AttackAction atk = autoAttack();
			if(atk == null)
			{
				return tryMove(enemyCastleLocs[0][0], enemyCastleLocs[0][1]);
			}
			else
			{
				return atk;
			}
		}

		return null;
	}


	private Action prophet()
	{
		return null;
	}


	private Action preacher()
	{
		return null;
	}


	private void getFMap() // makes fullMap
	{
		boolean[][] m = getPassableMap();
		boolean[][] k = getKarboniteMap();
		boolean[][] f = getFuelMap();

		fullMap = new int[m.length][m.length];

		int h = m.length;
		int w = h;

		for(int i = 0; i < h; i++)
		{
			for(int j = 0; j < w; j++)
			{
				if(!m[i][j])
				{
					fullMap[i][j] = IMPASSABLE;
				}
				else if(k[i][j])
				{
					fullMap[i][j] = KARBONITE;
				}
				else if(f[i][j])
				{
					fullMap[i][j] = FUEL;
				}
				else
				{
					fullMap[i][j] = PASSABLE;
				}
			}
		}
	}

	private boolean getReflDir() // set hRefl
	{
		int top = (fullMap.length + 1) / 2;
		int left = (fullMap[0].length + 1) / 2;

		for(int i = 0; i < top; i++)	// Goes through top left quarter of map and tests one cell at a time
		{								// for whether it's reflected horizontally then vertically.
			for(int j = 0; j < left; j++) // If a discrepancy is found, method returns.
			{
				if(fullMap[i][j] != fullMap[fullMap.length - 1 - i][j])
				{
					return true;
				}
				else if(fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j])
				{
					return false;
				}
			}
		}

		for(int i = fullMap.length; i > top; i--) // Checks bottom right quarter same way just in case no return yet.
		{
			for(int j = fullMap[0].length; j > left; j--)
			{
				if(fullMap[i][j] != fullMap[fullMap.length - 1 - i][j])
				{
					return true;
				}
				else if(fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j])
				{
					return false;
				}
			}
		}

		return true; // If it gets here, it's reflected both ways.
	}

	private void sendOwnLoc() // Call first and second turn for castles to send their location to other castles
	{
		int[] plain; // 0 is location on your half of map; 1 is how far across
		int[] encoded = new int[2]; // ditto above
		int temp;

		if(hRefl)
		{
			plain = new int[] {me.y, me.x};
		}
		else
		{
			plain = new int[] {me.x, me.y};
		}

		encodedLocError = 0;
		encodedLocError += plain[0] % 2;

		encoded[0] = (int) Math.floor(plain[0] / 2);
		if(plain[1] >= fullMap.length / 2) // Same thing for opposite sides of the map
		{
			temp = (plain[1] - (int) Math.floor(fullMap.length / 2) - 8);
			encodedLocError += 2 * (temp % 2);
			encoded[1] = (int) Math.floor(temp / 2);
		}
		else
		{
			temp = (plain[1] - 3);
			encodedLocError += 2 * (temp % 2);
			encoded[1] = (int) Math.floor(temp / 2);
		}

		if(encoded[1] >= 8)
		{
			log("encoded location across value was too big (it was " + encoded[1] + "), it has been set to 7.");
			encoded[1] = 7;
		}

		if(encoded[0] >= 32 || encoded[0] < 0)
		{
			log("oh no encoded[0] is " + encoded[0]);
		}
		if(encoded[1] >= 8 || encoded[1] < 0)
		{
			log("oh no encoded[1] is " + encoded[1]);
		}

		plainCastleLocs[0] = new int[] {me.x, me.y};
		encodedCastleLocs[0] = encoded[0] * 8 + encoded[1];
		castleTalk(encodedCastleLocs[0]);
	}

	private void decodeCastleLoc(int i) // Tell it which index of encodedCastleLocs to decode, it'll put result in corresponding
	{									// index of plainCastleLocs.
		int[] plain = new int[2];

		plain[0] = (int) Math.floor(encodedCastleLocs[i] / 8) * 2;

		if((hRefl && me.x < fullMap.length / 2) || (!hRefl && me.y < fullMap.length / 2))
		{
			plain[1] = ((int) Math.floor(encodedCastleLocs[i] % 8) * 2) + 3;
		}
		else
		{
			plain[1] = ((int) Math.floor(encodedCastleLocs[i] % 8) * 2) + (int) Math.floor(fullMap.length / 2) + 8;
		}


		if(hRefl)
		{
			plainCastleLocs[i][0] = plain[1];
			plainCastleLocs[i][1] = plain[0];
		}
		else
		{
			plainCastleLocs[i] = plain;
		}
	}

	private void fixLocError(int adjustment, int i) // Namely, the small error due to compression in stored location of other castles
	{
		if(hRefl)
		{
			plainCastleLocs[i][1] += adjustment % 2;
			plainCastleLocs[i][0] += (int) Math.floor(adjustment / 2);
		}
		else
		{
			plainCastleLocs[i][0] += adjustment % 2;
			plainCastleLocs[i][1] += (int) Math.floor(adjustment / 2);
		}
	}

	private void sendCastleLocs(int r2) // Whenever you make a pilgrim, call this. Will give
	{		// how far away pilgrim has to be in each direction to be closer to other castle.

		if(numCastles == 2)
		{
			signal(encodedCastleLocs[1] * 257, r2);
		}
		else if(numCastles == 3)
		{
			signal(encodedCastleLocs[1] * 256 + encodedCastleLocs[0], r2);
		}
		else if(numCastles != 1)
		{
			log("oh no numCastles is " + numCastles);
		}
	}

	private void getAllCastleLocs() // Call on first turn of unit to get locations of and number of castles
	{
		for(Robot rob : getVisibleRobots())
		{
			if(rob.unit == SPECS.CASTLE)
			{
				castleIDs[0] = rob.id;

				plainCastleLocs[0] = new int[] {rob.x, rob.y};

				if(isRadioing(rob))
				{
					encodedCastleLocs[1] = (int) Math.floor(rob.signal / 256);
					encodedCastleLocs[2] = rob.signal % 256;

					if(encodedCastleLocs[1] == encodedCastleLocs[2])
					{
						numCastles = 2;
						decodeCastleLoc(1);
					}
					else
					{
						numCastles = 3;
						decodeCastleLoc(1);
						decodeCastleLoc(2);
					}
				}
				else
				{
					numCastles = 1;
				}

				break;
			}
		}
	}

	private void getEnemyCastleLocs()
	{
		for(int i = 0; i < 3; i++)
		{
			if(hRefl)
			{
				enemyCastleLocs[i][0] = fullMap.length - 1 - plainCastleLocs[i][0];
				enemyCastleLocs[i][1] = plainCastleLocs[i][1];
			}
			else
			{
				enemyCastleLocs[i][0] = plainCastleLocs[i][0];
				enemyCastleLocs[i][1] = fullMap.length - 1 - plainCastleLocs[i][1];
			}
		}
	}

	private Robot[] getEnemiesInRange()
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> enms = new ArrayList<Robot>();

		int minRange, range;
		if(me.unit == 3)
		{
			minRange = 1;
			range = 16;
		}
		else if(me.unit == 4)
		{
			minRange = 16;
			range = 64;
		}
		else
		{
			log("you're trying to attack with a non-combat robot or a preacher and autoAttack() is not gonna work");
			minRange = 0;
			range = 0;
		}
		
		for(Robot rob : robs)
		{
			if(rob.team != me.team && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * 
					(rob.y - me.y) <= range && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) >= minRange)
			{
				enms.add(rob);
			}
		}

		return enms.toArray(new Robot[enms.size()]);
	}

	private AttackAction autoAttack() // NOT (well) TESTED: Attacks unit in attack range of type earliest in attackPriority, of lowest ID
	{
		Robot[] robs = getEnemiesInRange();
		
		if(robs.length == 0)
		{
			return null;
		}
		
		ArrayList<Robot> priorRobs = new ArrayList<Robot>(); // only robots of highest priority type
		boolean found = false;
		int i = 0;

		while(!found && i < 6) // make priorRobs
		{
			for(Robot rob : robs)
			{
				if(rob.unit == attackPriority[i])
				{
					found = true;
					priorRobs.add(rob);
				}
			}
			i++;
		}

		if(priorRobs.size() == 1)
		{
			return attack(priorRobs.get(0).x - me.x, priorRobs.get(0).y - me.y);
		}
		else if(priorRobs.size() == 0)
		{
			log("why are there no enemies and yet autoAttack() has gotten all the way here");
			return null;
		}

		int lowestID = 4097;
		for(int j = 0; j < priorRobs.size(); j++)
		{
			if(priorRobs.get(j).id < lowestID)
			{
				lowestID = priorRobs.get(j).id;
			}
		}

		return attack(getRobot(lowestID).x - me.x, getRobot(lowestID).y - me.y);
	}

	private MoveAction tryMove(int goalX, int goalY) {
		int radius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		int[][] moves;
		int index = 0;
		if (radius == 2) {
			moves = new int[12][3];
		} else if (radius == 3) {
			moves = new int[28][3];
		} else {
			log("uh oh they updated the specs, fix tryOrder");
		}
		for (int dx = -radius; dx <= radius; dx++) {
			int newX = me.x + dx;
			if (newX <= -1 || newX >= fullMap[0].length) {
				continue;
			}
			for (int dy = -radius; dy <= radius; dy++) {
				int newY = me.y + dy;
				if (newY <= -1 || newY >= fullMap.length || dx * dx + dy * dy > radius * radius
						|| (dx * dx + dy * dy) * (SPECS.UNITS[me.unit].FUEL_PER_MOVE) > fuel
						|| fullMap[newY][newX] == IMPASSABLE || robotMap[newY][newX] > 0) {
					continue;
				}
				moves[index++] = new int[] { dx, dy,
						(goalX - newX) * (goalX - newX) + (goalY - newY) * (goalY - newY) };
			}
		}
		if (index == 0) {
			return null;
		}
		int min = fullMap.length * fullMap.length + 1;
		int minIndex;
		for (int i = 0; i < index; i++) {
			if (moves[i][2] < min) {
				min = moves[i][2];
				minIndex = i;
			}
		}

		return move(moves[minIndex][0], moves[minIndex][1]);
	}
}