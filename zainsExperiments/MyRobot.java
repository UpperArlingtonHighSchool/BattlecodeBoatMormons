package bc19;

import bc19.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class MyRobot extends BCAbstractRobot {
	// important
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private boolean hRefl;
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel
	private int[][] robotMap;
	private int xorKey; // XOR any signal by this, and any castletalk by this % 256
	// Note: the encodedCastleLocs are sort of separate and thus XOR'd with this % 256
	// separately; don't worry 'bout it.
	private final int[][] adjacentSpaces = new int[][] { // Matrix of adjacent spaces, relative to the Robot
		new int[] {0,1},
		new int[] {-1,1},
		new int[] {-1,0},
		new int[] {-1,-1},
		new int[] {0,-1},
		new int[] {1,-1},
		new int[] {1,0},
		new int[] {1,1}
	};
	private int numCastles;
	private int ourDeadCastles = 0;
	private int[][] castleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int globalMinusLocalTurn;

	// for castles
	private int[] numUnits = new int[] {0, 0, 0, 0, 0};
	private int numFuelMines = 0;
	private int numKarbMines = 0;
	private int pilgrimLim;
	private int[] castleIDs = new int[] {-1, -1, -1}; // small so we don't worry about if there's only 1 or 2 castles
	private int numBuilders;

	// For pilgrims
	private ArrayList<int[]> karbosInUse = new ArrayList<>(); // logs karbos and fuels that other robots are on
	private ArrayList<int[]> fuelsInUse = new ArrayList<>(); // you should clear these whenever the unit returns to a castle
	private int home; // index of home castle

	// For pathing
	private ArrayList<int[]> currentPath = null;
	private int locInPath;

	// For lattice
	private int castleDir;
	private int sideDir;
	private boolean arrived;

	// For attacking
	private final int[] attackPriority = new int[] {4, 5, 3, 0, 2, 1};
	private int targetCastle;

	// For castles, for communicating locations 
	private int[] sortedCastleIDs;
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
		if (me.turn == 1)
		{
			numCastles = 1;
			castleIDs[0] = me.id;

			for (Robot rob : getVisibleRobots()) {
				if (rob.team == me.team && rob.id != me.id) {
					castleIDs[numCastles] = rob.id;
					numCastles += 1;
				}
			}

			numBuilders = numCastles;
			pilgrimLim = (int) Math.floor(Math.min(numFuelMines * 1.25, numFuelMines * .75 + numKarbMines)) - numCastles;

			sortedCastleIDs = Arrays.copyOf(castleIDs, 3);
			sortcastleIDs();

			if (numCastles > 1) {
				sendOwnLoc();

				// Castle location error stuff
				int tm = (me.team * 2 - 1);
				int offer = lastOffer[me.team][0] * tm;

				if (offer == 0)
				{
					castleErrorsCatalogued = 0;
					return proposeTrade(((encodedLocErrors[0] << 2) + getCastleNum(0) + 1) * tm, 0);
				}
				else if (offer < 32) // will actually range from 1 to 31, interestingly enough
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
				}
			}
			else
			{
				castleLocs[0] = new int[] {me.x, me.y};
			}
		}

		else if (me.turn == 2)
		{
			if (numCastles > 1) {
				sendOwnLoc();

				// castle loc error stuff
				int tm = (me.team * 2 - 1);

				int[] offers = new int[] {lastOffer[me.team][0] * tm, lastOffer[me.team][1] * tm};

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


		// Every turn

		// tell adjacents current numUnits[1]
		boolean haveNeighbors = false;
		checkNeighbors: for (int dx = -1; dx <= 1; dx++) {
			int tryX = me.x + dx;
			if (tryX <= -1 || tryX >= fullMap.length) {
				continue;
			}
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) {
					dy++;
				}
				int tryY = me.y + dy;
				if (tryY <= -1 || tryY >= fullMap.length) {
					continue;
				}
				if (robotMap[tryY][tryX] > 0) {
					haveNeighbors = true;
					break checkNeighbors;
				}
			}
		}
		if (haveNeighbors) {
			signal(numUnits[1], 2);
		}


		// Update numUnits[1] and castle deaths
		for (int i = 0; i < 3; i++) {
			int robotID = castleIDs[i];

			if(robotID == -1)
			{
				continue;
			}

			Robot castle = getRobot(robotID);
			if (castle == null) {
				ourDeadCastles += 1;
				castleIDs[i] = -1;
				castleLocs[i] = null;
			}

			if(me.turn > 3)
			{
				int talk = getRobot(robotID).castle_talk;
				if(talk >= 1 && talk <= 5)
				{
					numUnits[talk - 1] += 1;
				}
			}
		}

		// Just a log
		if(me.turn % 20 == 0)
		{
			log("Turn: " + me.turn + ". Pilgrim population: " + numUnits[1] + ". Prophet population:  " + numUnits[3] + ". Pilgrim limit: " + pilgrimLim + ".");
		}

		// Defend if under attack
		int[] atk = autoAttack();
		if(atk != null)
		{
			int[] loc = availAdjSq(new int[] {atk[0] > 0 ? 1 : (atk[0] < 0 ? -1 : 0), atk[1] > 0 ? 1 : (atk[1] < 0 ? -1 : 0)});

			if(karbonite >= 30 && fuel >= 50 && getRobot(robotMap[me.y + atk[1]][me.x + atk[0]]).unit != SPECS.PILGRIM && loc != null)
			{
				sendCastleLocs(loc[0] * loc[0] + loc[1] * loc[1]);
				castleTalk(5);
				return buildUnit(5, loc[0], loc[1]);
			}

			return attack(atk[0], atk[1]);
		}

		// Stop if you got no resources (leave enough resources to comm and for other pilgrims to mine too)
		if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + numUnits[1] + 2 || karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE)
		{
			return null;
		}

		// If there's enough pilgrims and some extra fuel (enough for all pilgrims to move max distance 1.5 times), build a prophet.
		if(numUnits[1] >= pilgrimLim)
		{
			if(me.turn < 850 && fuel >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_FUEL + 2 + numUnits[1] * 6 && karbonite >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_KARBONITE)
			{
				int doit; // If there's lots of resources, definitely build. If only a little, maybe build one.
				// This is so all castles and churches build about the same amount.
				if(fuel >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_FUEL * numBuilders + 2 + numUnits[1] * 6 && karbonite >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_KARBONITE * numCastles)
				{
					doit = 0;
				}
				else
				{
					doit = (int) (Math.random() * numBuilders);
				}

				if(doit == 0)
				{
					int[] loc = randomOddAdjSq();
					if(loc != null)
					{
						sendCastleLocs(loc[0] * loc[0] + loc[1] * loc[1]);
						castleTalk(4);
						return buildUnit(4, loc[0], loc[1]);
					}
				}
			}
			return null;
		}

		// Build a pilgrim
		int[] loc = randomAdjSq();

		if(loc != null)
		{
			castleTalk(2);
			sendCastleLocs(loc[0] * loc[0] + loc[1] * loc[1]);
			return buildUnit(SPECS.PILGRIM, loc[0], loc[1]);
		}

		return null; //default
	}

	private Action church() {
		return null;
	}

	private Action pilgrim() {
		if (me.turn == 1)
		{
			getAllCastleLocs();
			getEnemyCastleLocs();
			pilgrimLim = (int) Math.floor(Math.min(numFuelMines * 1.25, numFuelMines * .75 + numKarbMines)) - numCastles;
		}

		Robot castle = null; // Determine whether adjacent to a castle
		for (int dx = -1; dx <= 1; dx++) {
			int testX = me.x + dx;
			if (testX <= -1 || testX >= fullMap.length) {
				continue;
			}
			for (int dy = -1; dy <= 1; dy++) {
				int testY = me.y + dy;
				if (testY <= -1 || testY >= fullMap.length) {
					continue;
				}
				Robot maybe = getRobot(robotMap[testY][testX]);
				if (robotMap[testY][testX] > 0 && maybe.unit == SPECS.CASTLE && maybe.team == me.team) {
					castle = maybe;
					karbosInUse.clear();
					fuelsInUse.clear();
					if (isRadioing(castle)) {
						numUnits[1] = castle.signal;
					}
				}
			}
		}

		if (currentPath != null && currentPath.size() > locInPath) // Continue on path
		{
			int[] nextMove = currentPath.get(locInPath);

			if (robotMap[nextMove[1]][nextMove[0]] <= 0)
			{
				int dx = nextMove[0] - me.x;
				int dy = nextMove[1] - me.y;
				if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE + pilgrimLim * 0.7) // leave fuel for some mining
				{
					locInPath += 1;
					return move(dx, dy);
				}
			}
		}

		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY // Give to castle or find new path to castle when at carrying capacity
				|| me.fuel == SPECS.UNITS[SPECS.PILGRIM].FUEL_CAPACITY) {
			if (castle != null) {
				return give(castle.x - me.x, castle.y - me.y, me.karbonite, me.fuel);
			}

			currentPath = bfs(castleLocs[home][0], castleLocs[home][1]);
			if (currentPath == null)
			{
				log("Pilgrim BFS returned null.");
				if(fuel >= pilgrimLim) // leave fuel for mining
				{
					int[] move = randomAdjSq();
					return move(move[0], move[1]);
				}
				else
				{
					return null;
				}
			}

			int[] nextMove = currentPath.get(0);
			int dx = nextMove[0] - me.x;
			int dy = nextMove[1] - me.y;
			if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE + pilgrimLim * 0.7) // leave fuel for some mining
			{
				locInPath += 1;
				return move(dx, dy);
			}
			return null;
		}

		if (fullMap[me.y][me.x] == KARBONITE || fullMap[me.y][me.x] == FUEL) // Mine if possible
		{
			if (fuel == 0)
			{
				log("can't mine b/c no fuel :'(");
				return null;
			}
			return mine();
		}

		int[] location; // Find next mine to go to
		if (20 * numUnits[1] > fuel) {
			location = findClosestFuel();
		} else {
			location = findClosestKarbo();
		}
		if (location == null) {
			location = castleLocs[home];
		}

		currentPath = bfs(location[0], location[1]); // Actually go there
		if (currentPath == null) {
			log("Pilgrim BFS returned null. 2");
			return null;
		}
		int[] nextMove = currentPath.get(locInPath);

		int dx = nextMove[0] - me.x;
		int dy = nextMove[1] - me.y;
		if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE + 5) // leave fuel for some mining
		{
			locInPath += 1;
			return move(dx, dy);
		}

		return null;
	}

	private Action crusader()
	{
		if (me.turn == 1)
		{
			getAllCastleLocs();
			getEnemyCastleLocs();
			pilgrimLim = (int) Math.floor(Math.min(numFuelMines * 1.25, numFuelMines * .75 + numKarbMines)) - numCastles;
		}

		return null;
	}

	private Action prophet()
	{
		if (me.turn == 1)
		{
			getAllCastleLocs();
			getEnemyCastleLocs();
			pilgrimLim = (int) Math.floor(Math.min(numFuelMines * 1.25, numFuelMines * .75 + numKarbMines)) - numCastles;
			getTargetCastle();
			getCastleDir();
			if(castleDir % 2 == 0)
			{
				sideDir = (((int) (Math.random() * 2)) * 4 + castleDir + 2) % 8;
			}
			arrived = false;
		}

		int[] atk = autoAttack();
		if(atk != null)
		{
			if(fuel >= 25)
			{
				return attack(atk[0], atk[1]);
			}
			else
			{
				return null;
			}
		}

		if(me.turn + globalMinusLocalTurn >= 850)
		{
			updateTargetCastle();

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
			{
				currentPath = bfs(enemyCastleLocs[targetCastle][0], enemyCastleLocs[targetCastle][1]);
			}

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
			{
				log("Prophet BFS returned null (or something invalid).");
				if(fuel >= pilgrimLim * 2) // leave fuel for mining
				{
					int[] mov = randomAdjSq();

					if(mov != null)
					{
						return move(mov[0], mov[1]);
					}

					return null;
				}
				else
				{
					return null;
				}
			}

			int[] mov = new int[] {currentPath.get(locInPath)[0] - me.x, currentPath.get(locInPath)[1] - me.y};

			if(fuel >= (mov[0] * mov[0] + mov[1] * mov[1]) * 2 + pilgrimLim * .7)
			{
				locInPath += 1;
				return move(mov[0], mov[1]);
			}
			else
			{	
				return null;
			}
		}
		else
		{
			if(!arrived && fuel >= pilgrimLim * 2)
			{
				int[] mov = latticify();
				if(mov != null)
				{
					arrived = true;
					return move(mov[0], mov[1]);
				}

				mov = exploreLattice();
				if(mov != null)
				{
					return move(mov[0], mov[1]);
				}
			}
		}
		return null;
	}

	private Action preacher()
	{
		if (me.turn == 1)
		{
			getAllCastleLocs();
			getEnemyCastleLocs();
			pilgrimLim = (int) Math.floor(Math.min(numFuelMines * 1.25, numFuelMines * .75 + numKarbMines)) - numCastles;
			getTargetCastle();
			getCastleDir();
			if(castleDir % 2 == 0)
			{
				sideDir = (((int) (Math.random() * 2)) * 4 + castleDir + 2) % 8;
			}
		}

		AttackAction atk = preacherAttack();
		if(atk != null)
		{
			return atk;
		}

		if(me.turn + globalMinusLocalTurn >= 850)
		{
			updateTargetCastle();

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
			{
				currentPath = bfs(enemyCastleLocs[targetCastle][0], enemyCastleLocs[targetCastle][1]);
			}

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
			{
				log("Prophet BFS returned null (or something invalid).");
				if(fuel >= pilgrimLim * 2) // leave fuel for mining
				{
					int[] mov = randomAdjSq();

					if(mov != null)
					{
						return move(mov[0], mov[1]);
					}

					return null;
				}
				else
				{
					return null;
				}
			}

			int[] mov = new int[] {currentPath.get(locInPath)[0] - me.x, currentPath.get(locInPath)[1] - me.y};

			if(fuel >= (mov[0] * mov[0] + mov[1] * mov[1]) * 2 + pilgrimLim * .7)
			{
				locInPath += 1;
				return move(mov[0], mov[1]);
			}
			else
			{	
				return null;
			}
		}
		else
		{
			if(fuel >= pilgrimLim * 2)
			{
				if(moveAway())
				{
					int[] mov = exploreLattice();
					if(mov != null)
					{
						return move(mov[0], mov[1]);
					}
				}

			}
		}
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
					numKarbMines++;
					fullMap[i][j] = KARBONITE;
				} else if (f[i][j]) {
					numFuelMines++;
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
				if (fullMap[y][x] == FUEL) {
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

		castleLocs[0] = new int[] {me.x, me.y};
		encodedCastleLocs[0] ^= (xorKey % 256);
		castleTalk(encodedCastleLocs[0]);
	}

	private void sortcastleIDs() // Smoke and Mirrors track 4
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

	private void decodeCastleLoc(int i, int adjustment) // Tell it which index of encodedCastleLocs to decode, it'll put result in corresponding index of castleLocs.
	{
		int temp = (encodedCastleLocs[i] ^ (xorKey % 256)) * mapSizeClass[0] + adjustment;
		if(hRefl)
		{
			castleLocs[i][0] = (int) Math.floor(temp / (mapSizeClass[1] * 2)) * 2 + (me.x > fullMap.length / 2 ? ((int) Math.floor((fullMap.length + 1) / 2)) : 0);
			castleLocs[i][1] = (int) Math.floor((temp % (mapSizeClass[1] * 2)) / 2);
		}
		else
		{
			castleLocs[i][0] = (int) Math.floor((temp % (mapSizeClass[1] * 2)) / 2);
			castleLocs[i][1] = (int) Math.floor(temp / (mapSizeClass[1] * 2)) * 2 + (me.y > fullMap.length / 2 ? ((int) Math.floor((fullMap.length + 1) / 2)) : 0);
		}

		if(castleLocs[i][0] < 0 || castleLocs[i][0] >= fullMap.length || castleLocs[i][1] < 0 || castleLocs[i][1] >= fullMap.length)
		{
			log("Comm error.: " + castleLocs[i][0] + " " + castleLocs[i][1] + " " + me.unit);
		}
	}

	private void decodeCastleLoc(int i) // Tell it which index of encodedCastleLocs to decode, it'll put result in corresponding index of castleLocs.
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
				castleLocs[0] = new int[] {rob.x, rob.y};

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

				globalMinusLocalTurn = rob.turn - me.turn;
			}
		}
	}

	private void getEnemyCastleLocs()
	{
		for(int i = 0; i < 3; i++)
		{
			if(hRefl)
			{
				enemyCastleLocs[i][0] = fullMap.length - 1 - castleLocs[i][0];
				enemyCastleLocs[i][1] = castleLocs[i][1];
			}
			else
			{
				enemyCastleLocs[i][0] = castleLocs[i][0];
				enemyCastleLocs[i][1] = fullMap.length - 1 - castleLocs[i][1];
			}
		}
	}

	private void getTargetCastle()
	{
		if(numCastles == 1)
		{
			targetCastle = 0;
		}
		else if(numCastles == 2)
		{
			targetCastle = fullMap[17][22] == 0 ? 0 : 1;				
		}
		else if(numCastles == 3)
		{
			int minInd, maxInd;
			int min = 64;
			int max = -1;

			for(int i = 0; i < 3; i++)
			{
				if(enemyCastleLocs[i][hRefl ? 1 : 0] < min)
				{
					minInd = i;
					min = enemyCastleLocs[i][hRefl ? 1 : 0];
				}
				if(enemyCastleLocs[i][hRefl ? 1 : 0] > max)
				{
					maxInd = i;
					max = enemyCastleLocs[i][hRefl ? 1 : 0];
				}
			}

			enemyCastleLocs = new int[][] {enemyCastleLocs[minInd], enemyCastleLocs[3 - minInd - maxInd], enemyCastleLocs[maxInd]};

			targetCastle = fullMap[22][17] == 0 ? 2 : 0;
		}
		else
		{
			log("uh oh crusader numCastles is " + numCastles);
		}
	}

	private Robot[] getEnemiesInRange() {
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> enms = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) <= SPECS.UNITS[me.unit].ATTACK_RADIUS[1]
					&& (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) >= SPECS.UNITS[me.unit].ATTACK_RADIUS[0]) {
				enms.add(rob);
			}
		}

		return enms.toArray(new Robot[enms.size()]);
	}

	private int[] autoAttack() // Returns null if you don't have enough fuel!
	{
		if(fuel < SPECS.UNITS[me.unit].ATTACK_FUEL_COST)
		{
			return null;
		}

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
			return new int[] {priorRobs.get(0).x - me.x, priorRobs.get(0).y - me.y};
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

		return new int[] {getRobot(lowestID).x - me.x, getRobot(lowestID).y - me.y};
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
			if (rob.team != me.team && (rob.unit == SPECS.CRUSADER || rob.unit == SPECS.PREACHER || rob.unit == SPECS.CASTLE)) {
				enemies.add(rob);
			}
		}

		return enemies.toArray(new Robot[enemies.size()]);
	}

	// For preacherAttack()
	private Robot[] getEnemyChurches() // Does not return from outside of visibility range :(
	{ // Do not combine with other preacherAttack() helper methods. Ask Zain for elaboration if wanted.
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> buildings = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && rob.unit == SPECS.CHURCH)
			{
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

	public AttackAction preacherAttack() // UNTESTED: Returns best attack for a preacher. No ally damage, then kill enemies, then damage
	{ // enemy combat units, then damage enemy buildings. Returns null if you don't have enough fuel.
		if(fuel < SPECS.UNITS[5].ATTACK_FUEL_COST)
		{
			return null;
		}

		Robot[] killable = getPreacherKillableRobots();
		Robot[] allies = getAllies();

		int[][] attackLocs = new int[9][9];
		ArrayList<int[]> bestLocs = new ArrayList<int[]>();
		bestLocs.add(new int[] {0, 0, 0}); // x, y, value

		for (int y = 0; y < 9; y++) // Killable units
		{
			for (int x = 0; x < 9; x++) {
				if (x == 3 && y > 2 && y < 6) {
					x += 3;
				}

				attackLocs[y][x] = 0;

				for (Robot ally : allies) {
					if (squareContainsRobot(ally, x, y)) {
						attackLocs[y][x] = -100;
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
					bestLocs.add(new int[] { x, y, attackLocs[y][x] });
				} else if (attackLocs[y][x] == bestLocs.get(0)[2]) {
					bestLocs.add(new int[] { x, y, attackLocs[y][x] });
				}
			}
		}

		if (bestLocs.get(0)[2] == 0)
		{
			return null;
		}
		else if(bestLocs.size() == 1)
		{
			return attack(bestLocs.get(0)[0] - 4, bestLocs.get(0)[1] - 4);
		}

		Robot[] combat = getEnemyRobots();
		ArrayList<int[]> bestbestLocs = new ArrayList<int[]>();
		bestbestLocs.add(new int[] { 0, 0, -1 }); // x, y, new value

		for (int[] pos : bestLocs) // Tiebreakers based on most damage to other combat units
		{
			for (Robot rob : combat) {
				if (squareContainsRobot(rob, pos[0], pos[1])) {
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if (attackLocs[pos[1]][pos[0]] > bestbestLocs.get(0)[2]) {
				bestbestLocs.clear();
				bestbestLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			} else if (attackLocs[pos[1]][pos[0]] == bestbestLocs.get(0)[2]) {
				bestbestLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			}
		}

		if (bestbestLocs.size() == 1) {
			return attack(bestbestLocs.get(0)[0] - 4, bestbestLocs.get(0)[1] - 4);
		}

		Robot[] build = getEnemyChurches(); // write this to return all castles and churches
		ArrayList<int[]> goodLocs = new ArrayList<int[]>();
		goodLocs.add(new int[] { 0, 0, -1 }); // x, y, new value

		for (int[] pos : bestbestLocs) // Tiebreakers based on most damage to other combat units
		{
			for (Robot rob : build) {
				if (squareContainsRobot(rob, pos[0], pos[1])) {
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if (attackLocs[pos[1]][pos[0]] > goodLocs.get(0)[2]) {
				goodLocs.clear();
				goodLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			} else if (attackLocs[pos[1]][pos[0]] == goodLocs.get(0)[2]) {
				goodLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			}
		}

		if (goodLocs.size() == 1) {
			return attack(goodLocs.get(0)[0] - 4, goodLocs.get(0)[1] - 4);
		}


		int lowestID = 4097;
		int[] finalBestLoc = new int[] {-1, -1};

		for (int[] loc : goodLocs) // Tiebreaks to attack single robot with lowest ID
		{
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					int newX = me.x + loc[0] - 4 + dx;
					int newY = me.y + loc[1] - 4 + dy;
					if(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length)
					{
						continue;
					}
					int ID = robotMap[newY][newX];
					if (ID > 0 && ID < lowestID && getRobot(ID).team != me.team) {
						lowestID = ID;
						finalBestLoc = new int[] {loc[0], loc[1]};
					}
				}
			}
		}

		if(finalBestLoc[0] == -1)
		{
			log("aha!");
			return null;
		}

		return attack(finalBestLoc[0] - me.x, finalBestLoc[1] - me.y);
	}

	private void updateTargetCastle()
	{
		boolean castleKilled = true;
		int newX, newY;

		for(int dx = -2; dx <= 2; dx++)
		{
			for(int dy = -2; dy <= 2; dy++)
			{
				newX = enemyCastleLocs[targetCastle][0] + dx;
				newY = enemyCastleLocs[targetCastle][1] + dy;

				if(!(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length))
				{
					int ID = robotMap[newY][newX];
					if(ID != 0 && getRobot(ID).team != me.team)
					{
						castleKilled = false;
					}
				}
			}
		}

		if(castleKilled)
		{
			enemyCastleLocs[targetCastle] = new int[] {-1, -1};

			if(numCastles == 2)
			{
				targetCastle = 1 - targetCastle;
			}

			else if(enemyCastleLocs[1][0] == -1)
			{
				targetCastle = enemyCastleLocs[0][0] != -1 ? 0 : 2;
			}
			else
			{
				targetCastle = 1;
			}
		}
	}

	// bfs is reaaaally fast now
	private ArrayList<int[]> bfs(int goalX, int goalY) {
		locInPath = 0;

		boolean occupied = false;
		if (robotMap[goalY][goalX] > 0) {
			occupied = true;
		}
		int fuelCost = SPECS.UNITS[me.unit].FUEL_PER_MOVE;
		int maxRadius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		LinkedList<int[]> spots = new LinkedList<>();
		int[] spot = new int[] { me.x, me.y };
		int[] from = new int[fullMap.length * fullMap.length];
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
							&& (robotMap[y][x] <= 0/* || getRobot(robotMap[y][x]).unit < 2*/)) {
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
		int[][] robomap = robotMap;
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
		locInPath = 0;

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

	private int[] availAdjSq(int[] target)
	{
		int i;
		if(target[0] == 0)
		{
			i = target[1] * -2 + 2;
		}
		else if(target[0] == -1)
		{
			i = target[1] * -1 + 2;
		}
		else if(target[0] == 1)
		{
			i = target[1] + 6;
		}
		else // NOTE: THIS WILL NOT CATCH ALL INVALID TARGETS, ONLY INVALID TARGET[0]S
		{
			log("That is not a valid target for availAdjSq(). Returning null.");
			return null;
		}

		int newX = me.x + adjacentSpaces[i][0];
		int newY = me.y + adjacentSpaces[i][1];

		int delta = 1;
		int sign = 1;

		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0)
		{
			if(delta >= 8)
			{
				log("No adjacent movable spaces (from availAdjSq()).");
				return null;
			}

			i += delta * sign;
			i %= 8;

			newX = me.x + adjacentSpaces[i][0];
			newY = me.y + adjacentSpaces[i][1];

			delta += 1;
			sign *= -1;
		}

		return adjacentSpaces[i];
	}

	private int[] randomAdjSq()
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

			if(i >= 8)
			{
				//				log("No adjacent movable spaces (from randomAdjSq()).");
				return null;
			}
		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0);

		return adjacentSpaces[rand];
	}

	private int[] randomOddAdjSq()
	{
		int rand, newX, newY;
		int pos = 1 - (me.x + me.y) % 2;

		rand = ((int) (Math.random() * 4)) * 2 + 1 + pos;
		int i = 0;
		do
		{
			i++;
			if(i > 4)
			{
				return null;
			}

			rand += 2;
			rand %= 8;
			newX = me.x + adjacentSpaces[rand][0];
			newY = me.y + adjacentSpaces[rand][1];

		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0);

		return adjacentSpaces[rand];
	}

	private void getCastleDir()
	{
		if(castleLocs[0][0] - me.x == 0)
		{
			castleDir = castleLocs[0][1] - me.y * -2 + 2;
		}
		else if(castleLocs[0][0] - me.x == -1)
		{
			castleDir = castleLocs[0][1] - me.y * -1 + 2;
		}
		else if(castleLocs[0][0] - me.x == 1)
		{
			castleDir = castleLocs[0][1] - me.y + 6;
		}
	}

	private int[] exploreLattice()
	{
		int[] fpoo;
		int newX, newY;

		if(castleDir % 2 == 0)
		{
			int chooseDir = (int) (Math.random() * 2);
			fpoo = (chooseDir == 0) ? (adjacentSpaces[(castleDir + 4) % 8]) : adjacentSpaces[sideDir];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] != -1 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}

			fpoo = (chooseDir != 0) ? (adjacentSpaces[(castleDir + 4) % 8]) : adjacentSpaces[sideDir];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] != -1 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}
		}
		else
		{
			int chooseDir = ((int) (Math.random() * 2)) * 2 - 1;
			fpoo = adjacentSpaces[(castleDir + 4 + chooseDir) % 8];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] != -1 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}

			fpoo = adjacentSpaces[(castleDir + 4 - chooseDir) % 8];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] != -1 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}
		}

		return null;
	}

	private int[] latticify()
	{
		int newX, newY;
		int dir = ((int) (Math.random() * 4)) * 2;
		int i = 0;

		do
		{
			i++;
			if(i > 4)
			{
				return null;
			}
			newX = me.x + adjacentSpaces[dir][0];
			newY = me.y + adjacentSpaces[dir][1];

			dir += 2;
			dir %= 8;

			if(me.unit == 5)
			{
			}
		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0);

		return adjacentSpaces[(dir + 6) % 8];
	}

	private boolean moveAway()
	{
		for(int dx = -2; dx <= 2; dx++)
		{
			for(int dy = -2; dy <= 2; dy++)
			{
				if((Math.abs(dx) == 2 && Math.abs(dy) == 2) || (dx == 0 && dy == 0))
				{
					continue;
				}
				
				int newX = me.x + dx;
				int newY = me.y + dy;
				if(!(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length))
				{
					int ID = robotMap[newY][newX];
					if(ID > 0 && getRobot(ID).team == me.team && (getRobot(ID).unit == 4 || getRobot(ID).unit == 0  || getRobot(ID).unit == 1))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}