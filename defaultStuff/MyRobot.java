package bc19;

import bc19.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class MyRobot extends BCAbstractRobot {
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private boolean hRefl;
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel
	private int xorKey; // XOR any signal by this, and any castletalk by this % 256
	// Note: the encodedCastleLocs are sort of separate and thus XOR'd with this % 256
	// separately; don't worry 'bout it.

	private int numOfUnits = 0;
	private int numOfMines = 0;
	private ArrayList<int[]> karbosInUse = new ArrayList<>(); // logs karbos and fuels that other robots are on
	private ArrayList<int[]> fuelsInUse = new ArrayList<>(); // you should clear these whenever the unit returns to a castle
	private int[][] robotMap;

	private ArrayList<int[]> currentPath = new ArrayList<>();
	private int locInPath;
	private int home; // index of home castle
	private final int[][] adjacentSpaces = new int[][] { //Matrix of adjacent spaces, relative to the Robot
		new int[] {0,1},
		new int[] {-1,1},
		new int[] {-1,0},
		new int[] {-1,-1},
		new int[] {0,-1},
		new int[] {1,-1},
		new int[] {1,0},
		new int[] {1,1}
	};

	private final int[] attackPriority = new int[] {4, 5, 3, 0, 2, 1};

	private int numCastles;
	private int ourDeadCastles = 0;
	private int[] castleIDs = new int[] {-1, -1, -1}; // small so we don't worry about if there's only 1 or 2 castles
	private int[][] castleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}

	private int[] sortedcastleIDs;
	private int[] encodedCastleLocs = new int[3];
	private int[] mapSizeClass;
	private int[][] enemyCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int[] encodedLocErrors = new int[3]; // Only for use by castles in first few turns
	private int castleErrorsCatalogued;

	public Action turn() {
		if (me.turn == 1) {
			getFMap();
			hRefl = getReflDir();
			setMapSizeClass();
			setXorKey();

			/*		if(hRefl) // Testing hRefl and fullMap
			{
				log("hor");
			}
			else
			{
				log("vert");
			}

			String boop;
			for(int[] r : fullMap)
			{
				boop = "";
				for(int c : r)
				{
					boop += c + " ";
				}
				log(boop);
			}*/
		}
		robotMap = getVisibleRobotMap();
		switch (me.unit) {
		case 0:
			return castle();
		case 1:
			return church();
		case 2:
			return pilgrim();
		case 3:
			return crusader();
		case 4:
			return prophet();
		case 5:
			return preacher();
		}
		return null;
	}

	private Action castle() {
		if (me.turn == 1) {
			numCastles = 1;
			castleIDs[0] = me.id;

			for (Robot rob : getVisibleRobots()) {
				if (rob.team == me.team && rob.id != me.id) {
					castleIDs[numCastles] = rob.id;
					numCastles += 1;
				}
			}

			sortedCastleIDs = Arrays.copyOf(castleIDs,  3);
			sortCastleIDs();

			if (numCastles > 1) {
				sendOwnLoc();

				// Castle location error stuff
				int tm = (me.team * 2 - 1);
				int offer = lastOffer[me.team][0] * tm;

				log(offer + " " + ((encodedLocErrors[0] << 2) + getCastleNum(0) + 1));

				if(offer == 0)
				{
					castleErrorsCatalogued = 0;
					return proposeTrade(((encodedLocErrors[0] << 2) + getCastleNum(0) + 1) * tm, 0);
				}
				else if(offer < 32) // will actually range from 1 to 31, interestingly enough
				{
					castleErrorsCatalogued = 1;
					encodedLocErrors[(offer - 1) % 4] = ((offer - 1) >> 2);
					return proposeTrade(((offer << 5) + (encodedLocErrors[0] << 2) + getCastleNum(0) + 1) * tm, 0);
				}
				else
				{
					castleErrorsCatalogued = 2;
					encodedLocErrors[(offer - 1) % 4] = ((offer - 1) >> 2);
					encodedLocErrors[((offer % 32) - 1) % 4] = (((offer % 32) - 1) >> 2);
					return proposeTrade(offer * tm, ((encodedLocErrors[0] << 2) + getCastleNum(0) + 1) * tm);
				}//*/
			}
			else
			{
				plainCastleLocs[0] = new int[] {me.x, me.y};
			}
		}

		else if (me.turn == 2) {
			if (numCastles > 1) {
				sendOwnLoc();

				// castle loc error stuff
				int tm = (me.team * 2 - 1);
				int[] offers = new int[] {lastOffer[me.team][0] * tm, lastOffer[me.team][1] * tm};
				
				log("errors catalogued: " + castleErrorsCatalogued);
				
				if(castleErrorsCatalogued == 0)
				{
					encodedLocErrors[((offers[0] % 32) - 1) % 4] = (((offers[0] % 32) - 1) >> 2);
					encodedLocErrors[(offers[1] - 1) % 4] = ((offers[1] - 1) >> 2);
				}
				else if(castleErrorsCatalogued == 1)
				{
					encodedLocErrors[(offers[1] - 1) % 4] = ((offers[1] - 1) >> 2);
				}
			}
			
			for (int i = 1; i < numCastles; i++)
			{
				encodedCastleLocs[i] = getRobot(castleIDs[i]).castle_talk;
				decodeCastleLoc(i, encodedLocErrors[getCastleNum(i)]);
			}

			getEnemyCastleLocs();
		}

		return null; //default
	}

	private Action church() {
		return null;
	}

	private Action pilgrim() {
		if (me.turn == 1) {
			getAllCastleLocs();
			getEnemyCastleLocs();

			/*String str  = "{"; // Testing that pilgrims know where all castles are 
			for(int i = 0; i < numCastles; i++)
			{
				str += "{";
				for(int j = 0; j < 2; j++)
				{
					str += enemyCastleLocs[i][j] + ", ";
				}
				str = str.substring(0, str.length() - 2) + "}, ";
			}
			str = str.substring(0, str.length() - 2) + "}";
			log(str);*/
		}
		return null;
	}

	private Action crusader() {
		return null;
	}

	private Action prophet() {
		return null;
	}

	private Action preacher() {
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

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				if (!m[i][j]) {
					fullMap[i][j] = IMPASSABLE;
				} else if (k[i][j]) {
					numOfMines++;
					fullMap[i][j] = KARBONITE;
				} else if (f[i][j]) {
					numOfMines++;
					fullMap[i][j] = FUEL;
				} else {
					fullMap[i][j] = PASSABLE;
				}
			}
		}
	}

	private boolean getReflDir() // set hRefl
	{
		int top = (fullMap.length + 1) / 2;
		int left = (fullMap[0].length + 1) / 2;

		for (int i = 0; i < top; i++) // Goes through top left quarter of map and tests one cell at a time
		{ // for whether it's reflected horizontally then vertically.
			for (int j = 0; j < left; j++) // If a discrepancy is found, method returns.
			{
				if (fullMap[i][j] != fullMap[fullMap.length - 1 - i][j]) {
					return true;
				} else if (fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j]) {
					return false;
				}
			}
		}

		for (int i = fullMap.length; i > top; i--) // Checks bottom right quarter same way just in case no return yet.
		{
			for (int j = fullMap[0].length; j > left; j--) {
				if (fullMap[i][j] != fullMap[fullMap.length - 1 - i][j]) {
					return true;
				} else if (fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j]) {
					return false;
				}
			}
		}

		log("it's frickin reflected both ways >:(");
		return true;
	}

	private int[] findClosestKarbo() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans;
		for (int x = 0; x < fullMap[0].length; x++) {
			looping: for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE) {
					int[] temp = new int[] { x, y };
					for (int[] out : karbosInUse) {
						if (out[0] == temp[0] && out[1] == temp[1]) {
							if (robotMap[y][x] == 0) {
								karbosInUse.remove(out);
							} else {
								continue looping;
							}
						}
					}
					if (robotMap[y][x] > 0) {
						karbosInUse.add(temp);
						continue looping;
					}
					int dx = x - me.x;
					int dy = y - me.y;
					if (dx * dx + dy * dy < minDistance) {
						ans = temp;
						minDistance = dx * dx + dy * dy;
					}
				}
			}
		}
		return ans;
	}


	private int[] findClosestFuel() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = new int[] { 0, 0 };
		for (int x = 0; x < fullMap[0].length; x++) {
			looping: for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE) {
					int[] temp = new int[] { x, y };
					for (int[] out : fuelsInUse) {
						if (out[0] == temp[0] && out[1] == temp[1]) {
							if (robotMap[y][x] == 0) {
								fuelsInUse.remove(out);
							} else {
								continue looping;
							}
						}
					}
					if (robotMap[y][x] > 0) {
						fuelsInUse.add(temp);
						continue looping;
					}
					int dx = x - me.x;
					int dy = y - me.y;
					if (dx * dx + dy * dy < minDistance) {
						ans = temp;
						minDistance = dx * dx + dy * dy;
					}
				}
			}
		}
		return ans;
	}

	private void setMapSizeClass()
	{
		int temp = fullMap.length;

		if(temp == 32)
		{
			mapSizeClass = new int[] {2, 32};
		}
		else if(temp <= 37)
		{
			mapSizeClass = new int[] {3, 36};
		}
		else if(temp <= 45)
		{
			mapSizeClass = new int[] {4, 44};
		}
		else if(temp <= 50)
		{
			mapSizeClass = new int[] {5, 50};
		}
		else if(temp <= 55)
		{
			mapSizeClass = new int[] {6, 54};
		}
		else if(temp <= 57)
		{
			mapSizeClass = new int[] {7, 56};
		}
		else
		{
			mapSizeClass = new int[] {8, 64};
		}
	}

	private void setXorKey()
	{
		int[] parts = new int[4];
		parts[0] = 5 + fullMap[9][30] + fullMap[18][8] + fullMap[9][0] + fullMap[23][28] + fullMap[15][31];
		parts[1] = 5 + fullMap[19][3] + fullMap[31][8] + fullMap[10][26] + fullMap[11][11] + fullMap[4][2];
		parts[2] = 5 + fullMap[6][9] + fullMap[4][20] + fullMap[13][3] + fullMap[18][29] + fullMap[19][12];
		parts[3] = 5 + fullMap[30][10] + fullMap[31][31] + fullMap[0][0] + fullMap[5][15] + fullMap[1][8];

		xorKey = parts[3] * 4096 + parts[2] * 256 + parts[1] * 16 + parts[0];
	}

	private void sendOwnLoc() // Call first and second turn for castles to send their location to other castles
	{
		if(hRefl)
		{
			int realX = me.x % ((int) Math.floor((fullMap.length + 1) / 2));
			int temp = realX % 2 + (int) Math.floor(realX / 2) * mapSizeClass[1] * 2  + 2 * me.y;
			encodedCastleLocs[0] = (int) Math.floor(temp / mapSizeClass[0]);
			encodedLocErrors[0] = temp %  mapSizeClass[0];
		}
		else
		{
			int realY = me.y % ((int) Math.floor((fullMap.length + 1) / 2));
			int temp = realY % 2 + (int) Math.floor(realY / 2) * mapSizeClass[1] * 2 + 2 * me.x;
			encodedCastleLocs[0] = (int) Math.floor(temp / mapSizeClass[0]);
			encodedLocErrors[0] = temp %  mapSizeClass[0];
		}

		plainCastleLocs[0] = new int[] {me.x, me.y};
		encodedCastleLocs[0] ^= (xorKey % 256);
		castleTalk(encodedCastleLocs[0]);
	}

	private void sortCastleIDs() // Smoke and Mirrors track 4
	{
		if(sortedCastleIDs[1] > sortedCastleIDs[0])
		{
			int temp = sortedCastleIDs[1];
			sortedCastleIDs[1] = sortedCastleIDs[0];
			sortedCastleIDs[0] = temp;
		}
		if(sortedCastleIDs[2] > sortedCastleIDs[1])
		{
			int temp = sortedCastleIDs[2];
			sortedCastleIDs[2] = sortedCastleIDs[1];
			sortedCastleIDs[1] = temp;
		}
		if(sortedCastleIDs[1] > sortedCastleIDs[0])
		{
			int temp = sortedCastleIDs[1];
			sortedCastleIDs[1] = sortedCastleIDs[0];
			sortedCastleIDs[0] = temp;
		}
	}
	
	private int getCastleNum(int index)
	{
		for(int i = 0; i < 3; i++)
		{
			if(sortedCastleIDs[i] == castleIDs[index])
			{
				return i;
			}
		}
	}

	private void decodeCastleLoc(int i, int adjustment) // Tell it which index of encodedCastleLocs to decode, it'll put result in corresponding index of plainCastleLocs.
	{
		int temp = (encodedCastleLocs[i] ^ (xorKey % 256)) * mapSizeClass[0] + adjustment;
		if(hRefl)
		{
			plainCastleLocs[i][0] = (int) Math.floor(temp / (mapSizeClass[1] * 2)) * 2 + (me.x > fullMap.length / 2 ? ((int) Math.floor((fullMap.length + 1) / 2)) : 0);
			plainCastleLocs[i][1] = (int) Math.floor((temp % (mapSizeClass[1] * 2)) / 2);
		}
		else
		{
			plainCastleLocs[i][0] = (int) Math.floor((temp % (mapSizeClass[1] * 2)) / 2);
			plainCastleLocs[i][1] = (int) Math.floor(temp / (mapSizeClass[1] * 2)) * 2 + (me.y > fullMap.length / 2 ? ((int) Math.floor((fullMap.length + 1) / 2)) : 0);
		}

		if(plainCastleLocs[i][0] < 0 || plainCastleLocs[i][0] >= fullMap.length || plainCastleLocs[i][1] < 0 || plainCastleLocs[i][1] >= fullMap.length)
		{
			log("Comm error.: " + plainCastleLocs[i][0] + " " + plainCastleLocs[i][1]);
		}
	}

	private void decodeCastleLoc(int i) // Tell it which index of encodedCastleLocs to decode, it'll put result in corresponding index of plainCastleLocs.
	{
		decodeCastleLoc(i, (int) Math.floor(mapSizeClass[0] / 2));
	}

	private void sendCastleLocs(int r2) // Whenever you make a pilgrim, call this. Will give
	{		// how far away pilgrim has to be in each direction to be closer to other castle.

		if(numCastles == 2)
		{
			signal(encodedCastleLocs[1] * 257, r2);
		}
		else if(numCastles == 3)
		{
			signal(encodedCastleLocs[1] * 256 + encodedCastleLocs[2], r2);
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
				plainCastleLocs[0] = new int[] {rob.x, rob.y};

				if(isRadioing(rob))
				{
					encodedCastleLocs[1] = (int) Math.floor(rob.signal / 256);
					encodedCastleLocs[2] = (int) Math.floor(rob.signal % 256);

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

	private Robot[] getEnemiesInRange() {
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> enms = new ArrayList<Robot>();

		int minRange, range;
		if (me.unit == 3) {
			minRange = 1;
			range = 16;
		} else if (me.unit == 4) {
			minRange = 16;
			range = 64;
		} else {
			log("you're trying to attack with a non-combat robot or a preacher and autoAttack() is not gonna work");
			minRange = 0;
			range = 0;
		}

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) <= range
					&& (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) >= minRange) {
				enms.add(rob);
			}
		}

		return enms.toArray(new Robot[enms.size()]);
	}

	private AttackAction autoAttack() // NOT (well) TESTED: Attacks unit in attack range of type earliest in
	// attackPriority, of lowest ID
	{
		Robot[] robs = getEnemiesInRange();

		if (robs.length == 0) {
			return null;
		}

		ArrayList<Robot> priorRobs = new ArrayList<Robot>(); // only robots of highest priority type
		boolean found = false;
		int i = 0;

		while (!found && i < 6) // make priorRobs
		{
			for (Robot rob : robs) {
				if (rob.unit == attackPriority[i]) {
					found = true;
					priorRobs.add(rob);
				}
			}
			i++;
		}

		if (priorRobs.size() == 1) {
			return attack(priorRobs.get(0).x - me.x, priorRobs.get(0).y - me.y);
		} else if (priorRobs.size() == 0) {
			log("why are there no enemies and yet autoAttack() has gotten all the way here");
			return null;
		}

		int lowestID = 4097;
		for (int j = 0; j < priorRobs.size(); j++) {
			if (priorRobs.get(j).id < lowestID) {
				lowestID = priorRobs.get(j).id;
			}
		}

		return attack(getRobot(lowestID).x - me.x, getRobot(lowestID).y - me.y);
	}

	// For preacherAttack()
	private Robot[] getPreacherKillableRobots() // Now returns only units with max health <= 20 in visibility range, but
	// can be edited
	{ // to also return damaged units or units 1 space outside visibility range
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> killable = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.unit == SPECS.PILGRIM || rob.unit == SPECS.PROPHET)) {
				killable.add(rob);
			}
		}

		return killable.toArray(new Robot[killable.size()]);
	}

	// For preacherAttack()
	private Robot[] getAllies() // Now returns only visible allies, but preachers can damage non-visible allies
	// :( plis update
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> allies = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team == me.team) {
				allies.add(rob);
			}
		}

		return allies.toArray(new Robot[allies.size()]);
	}

	// For preacherAttack()
	private Robot[] getEnemyRobots() // Does not return from outside of visibility range :(
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> enemies = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.unit == SPECS.CRUSADER || rob.unit == SPECS.PREACHER)) {
				enemies.add(rob);
			}
		}

		return enemies.toArray(new Robot[enemies.size()]);
	}

	// For preacherAttack()
	private Robot[] getEnemyBuildings() // Does not return from outside of visibility range :(
	{ // Do not combine with other preacherAttack() helper methods. Ask Zain for
		// elaboration if wanted.
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> buildings = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.unit == SPECS.CASTLE || rob.unit == SPECS.CHURCH)) {
				buildings.add(rob);
			}
		}

		return buildings.toArray(new Robot[buildings.size()]);
	}

	// For preacherAttack()
	private boolean squareContainsRobot(Robot rob, int centerX, int centerY) // 3x3 square
	{
		if (rob.x + 1 >= centerX && rob.x - 1 <= centerX && rob.y + 1 >= centerY && rob.y - 1 <= centerY) {
			return true;
		}
		return false;
	}

	public AttackAction preacherAttack() // UNTESTED: Returns best attack for a preacher. No ally damage, then kill
	// enemies, then damage
	{ // enemy combat units, then damage enemy buildings
		Robot[] killable = getPreacherKillableRobots();
		Robot[] allies = getAllies();

		int[][] attackLocs = new int[9][9];
		ArrayList<Integer[]> bestLocs = new ArrayList<Integer[]>();
		bestLocs.add(new Integer[] { 0, 0, -1 }); // x, y, value

		for (int y = 0; y < 9; y++) // Killable units
		{
			for (int x = 0; x < 9; x++) {
				if (x == 3 && y > 2 && y < 6) {
					x += 3;
				}

				attackLocs[y][x] = 0;

				for (Robot ally : allies) {
					if (squareContainsRobot(ally, x, y)) {
						attackLocs[y][x] = -1;
					}
				}

				if (attackLocs[y][x] == 0) {
					for (Robot deathable : killable) {
						if (squareContainsRobot(deathable, x, y)) {
							attackLocs[y][x] += 1;
						}
					}
				}

				if (attackLocs[y][x] > bestLocs.get(0)[2]) {
					bestLocs.clear();
					bestLocs.add(new Integer[] { x, y, attackLocs[y][x] });
				} else if (attackLocs[y][x] == bestLocs.get(0)[2]) {
					bestLocs.add(new Integer[] { x, y, attackLocs[y][x] });
				}
			}
		}
		if (bestLocs.size() == 1) {
			return attack(bestLocs.get(0)[0] - 4, bestLocs.get(0)[1] - 4);
		}

		Robot[] combat = getEnemyRobots(); // write this to return all crusaders and preachers
		ArrayList<Integer[]> bestbestLocs = new ArrayList<Integer[]>();
		bestLocs.add(new Integer[] { 0, 0, -1 }); // x, y, new value

		for (Integer[] pos : bestLocs) // Tiebreakers based on most damage to other combat units
		{
			for (Robot rob : combat) {
				if (squareContainsRobot(rob, pos[0], pos[1])) {
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if (attackLocs[pos[1]][pos[0]] > bestbestLocs.get(0)[2]) {
				bestbestLocs.clear();
				bestbestLocs.add(new Integer[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			} else if (attackLocs[pos[1]][pos[0]] == bestbestLocs.get(0)[2]) {
				bestbestLocs.add(new Integer[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			}
		}

		if (bestbestLocs.size() == 1) {
			return attack(bestbestLocs.get(0)[0] - 4, bestbestLocs.get(0)[1] - 4);
		}

		Robot[] build = getEnemyBuildings(); // write this to return all castles and churches
		ArrayList<Integer[]> goodLocs = new ArrayList<Integer[]>();
		bestLocs.add(new Integer[] { 0, 0, -1 }); // x, y, new value

		for (Integer[] pos : bestbestLocs) // Tiebreakers based on most damage to other combat units
		{
			for (Robot rob : build) {
				if (squareContainsRobot(rob, pos[0], pos[1])) {
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if (attackLocs[pos[1]][pos[0]] > goodLocs.get(0)[2]) {
				goodLocs.clear();
				goodLocs.add(new Integer[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			} else if (attackLocs[pos[1]][pos[0]] == goodLocs.get(0)[2]) {
				goodLocs.add(new Integer[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			}
		}

		if (goodLocs.size() == 1) {
			return attack(goodLocs.get(0)[0] - 4, goodLocs.get(0)[1] - 4);
		}

		int lowestID = 4097;
		int[] finalBestLoc;
		int[][] robMap = getVisibleRobotMap();

		for (Integer[] loc : goodLocs) // Tiebreaks to attack single robot with lowest ID
		{
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					int ID = robMap[loc[1] + dy][loc[0] + dx];

					if (ID > 0 && ID < lowestID) {
						lowestID = ID;
						finalBestLoc = new int[] { loc[0] + dx, loc[1] + dy };
					}
				}
			}
		}

		return attack(finalBestLoc[0] - 4, finalBestLoc[1] - 4);
	}

	// bfs is reaaaally fast now
	private ArrayList<int[]> bfs(int goalX, int goalY) {
		boolean occupied = false;
		if (robotMap[goalY][goalX] > 0) {
			occupied = true;
		}
		int fuelCost = SPECS.UNITS[me.unit].FUEL_PER_MOVE;
		int maxRadius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		LinkedList<int[]> spots = new LinkedList<>();
		int[] spot = new int[] { me.x, me.y };
		int[] from = new int[fullMap.length * fullMap[0].length];
		for (int i = 0; i < from.length; i++) {
			from[i] = -1;
		}

		// these two are only used if occupied == true
		int[] closestSpot = null;
		int closestDistance = (goalX - me.x) * (goalX - me.x) + (goalY - me.y) * (goalY - me.y);

		while (!(spot[0] == goalX && spot[1] == goalY)) {
			int left = Math.max(0, spot[0] - maxRadius);
			int top = Math.max(0, spot[1] - maxRadius);
			int right = Math.min(fullMap[0].length - 1, spot[0] + maxRadius);
			int bottom = Math.min(fullMap.length - 1, spot[1] + maxRadius);

			for (int x = left; x <= right; x++) {
				int dx = x - spot[0];
				for (int y = top; y <= bottom; y++) {
					int dy = y - spot[1];
					if (dx * dx + dy * dy <= maxRadius * maxRadius && fullMap[y][x] > IMPASSABLE
							&& (robotMap[y][x] <= 0 || getRobot(robotMap[y][x]).unit < 2)) {
						if (from[y * fullMap.length + x] != -1) {
							continue;
						}
						int[] newSpot = new int[] { x, y };
						from[y * fullMap.length + x] = spot[1] * fullMap.length + spot[0];

						if (occupied) {
							if ((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) < closestDistance) {
								closestDistance = (goalX - x) * (goalX - x) + (goalY - y) * (goalY - y);
								closestSpot = newSpot;
								continue;
							}
						}
						spots.add(newSpot);
					}
				}
			}

			if (occupied && closestSpot != null) {
				spot = closestSpot;
				break;
			}

			spot = spots.poll();
			if (spot == null) {
				// log("exhausted all options");
				return null;
			}
		}
		ArrayList<int[]> ans = new ArrayList<>();
		while (from[spot[1] * fullMap.length + spot[0]] != -1) {
			ans.add(0, spot);
			int prevSpot = from[spot[1] * fullMap.length + spot[0]];
			spot = new int[] { prevSpot % fullMap.length, (int) (prevSpot / fullMap.length) };
		}
		return ans;
	}
	
	private boolean spaceIsCootiesFree(int x, int y) {
		int[][] robomap = getVisibleRobotMap();
		for (int[] adj : adjacentSpaces) {
			if (y + adj[1] > -1 && y + adj[1] < robomap.length && x + adj[0] > -1 && x + adj[0] < robomap.length)
			{
				if (robomap[y + adj[1]][x + adj[0]] > 0) {
					return false;
				}
			}
		}
		return true;
	}

	// mormons may be polygamists but no touchie touchie
	private ArrayList<int[]> bfsCooties(int goalX, int goalY) {
		boolean occupied = false;
		if (robotMap[goalY][goalX] > 0) {
			occupied = true;
		}
		// int fuelCost = SPECS.UNITS[me.unit].FUEL_PER_MOVE;
		int maxRadius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		LinkedList<int[]> spots = new LinkedList<>();
		int[] spot = new int[] { me.x, me.y };
		int[] from = new int[fullMap.length * fullMap[0].length];
		for (int i = 0; i < from.length; i++) {
			from[i] = -1;
		}
		// these two are only used if occupied == true
		int[] closestSpot = null;
		int closestDistance = (goalX - me.x) * (goalX - me.x) + (goalY - me.y) * (goalY - me.y);

		while (!(spot[0] == goalX && spot[1] == goalY)) {
			int left = Math.max(0, spot[0] - maxRadius);
			int top = Math.max(0, spot[1] - maxRadius);
			int right = Math.min(fullMap[0].length - 1, spot[0] + maxRadius);
			int bottom = Math.min(fullMap.length - 1, spot[1] + maxRadius);

			for (int x = left; x <= right; x++) {
				int dx = x - spot[0];
				for (int y = top; y <= bottom; y++) {
					int dy = y - spot[1];
					if (dx * dx + dy * dy <= maxRadius * maxRadius 
							&& fullMap[y][x] > IMPASSABLE 
							&& robotMap[y][x] <= 0
							&& spaceIsCootiesFree(x, y)) {
						if (from[y * fullMap.length + x] != -1) {
							continue;
						}
						int[] newSpot = new int[] { x, y };
						from[y * fullMap.length + x] = spot[1] * fullMap.length + spot[0];

						if (occupied) {
							if ((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) < closestDistance) {
								closestDistance = (goalX - x) * (goalX - x) + (goalY - y) * (goalY - y);
								closestSpot = newSpot;
								continue;
							}
						}
						spots.add(newSpot);
					}
				}
			}
			if (occupied && closestSpot != null) {
				spot = closestSpot;
				break;
			}
			spot = spots.poll();
			if (spot == null) {
				//log("exhausted all options");
				return null;
			}
		}
		ArrayList<int[]> ans = new ArrayList<>();
		while (from[spot[1] * fullMap.length + spot[0]] != -1) {
			ans.add(0, spot);
			int prevSpot = from[spot[1] * fullMap.length + spot[0]];
			spot = new int[] { prevSpot % fullMap.length, (int) (prevSpot / fullMap.length) };
		}
		return ans;
	}
	

	private MoveAction randomAdjMove()
	{
		int rand, newX, newY;
		rand = (int) (Math.random() * 8);
		int i = 0;
		do
		{
			rand += 1;
			rand %= 8;
			i++;
			newX = me.x + adjacentSpaces[rand][0];
			newY = me.y + adjacentSpaces[rand][1];
		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || getVisibleRobotMap()[newY][newX] > 0 && i < 8);

		if(i < 8)
		{
			return move(adjacentSpaces[rand][0], adjacentSpaces[rand][1]);
		}
		else
		{
			log("BFS failed and no adjacent movable spaces");
			return null;
		}
	}
}