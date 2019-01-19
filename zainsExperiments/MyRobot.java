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
	private final int[] attackPriority = new int[] { 4, 5, 3, 0, 1, 2 }; // 0: Castle, 1: Church, 2: Pilgrim, 3:
	// Crusader, 4: Prophet,
	private boolean hRefl; // true iff reflected horizontally 5: Preacher. Feel free to mess with order in
	// your robots.
	private ArrayList<int[]> karbosInUse = new ArrayList<>(); // logs karbos and fuels that other robots are on
	private ArrayList<int[]> fuelsInUse = new ArrayList<>(); // you should clear these whenever the unit returns to a castle
	private int[][] robotMap;
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel
	private int numCastles;
	private int[] mapSizeClass;
	private int[] castleIDs = new int[3]; // small so we don't worry about if there's only 1 or 2 castles
	private int[] sortedCastleIDs; // i swear don't even worry about it guys it]s fine
	private int[][] plainCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int[] encodedCastleLocs = new int[] {0, 0, 0};
	private int castleErrorsCatalogued;
	private int[][] enemyCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int[] encodedLocErrors = new int[3]; // Only for use by castles in first few turns
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


	private int xorKey; // XOR any signal by this, and any castletalk by this % 256
	// Note: the encodedCastleLocs are sort of separate and thus XOR'd with this % 256
	// separately; don't worry 'bout it.

	public Action turn() {
		if (me.turn == 1) {
			getFMap();
			hRefl = getReflDir();
			setMapSizeClass();
			setXorKey();

			/*if(hRefl) // Testing hRefl and fullMap
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
			
			sendCastleLocs(1);
			return buildUnit(2, 1, 0);
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

			String str  = "{"; // Testing that pilgrims know where all castles are 
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
			log(str);
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
					fullMap[i][j] = KARBONITE;
				} else if (f[i][j]) {
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

			if (!occupied && closestSpot != null) {
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
}