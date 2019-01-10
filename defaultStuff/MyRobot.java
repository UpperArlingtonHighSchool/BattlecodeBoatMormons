package bc19;

import bc19.*;
//import java.util.Arrays;

public class MyRobot extends BCAbstractRobot {
	public int turn;
	private boolean hRefl; // true iff reflected horizontally
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel

	public Action turn() {
		turn++;

		if(turn == 1)
		{
			getFMap();
			hRefl = getReflDir();
			
	/*		if(hRefl) // Testing
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

		if (me.unit == SPECS.CASTLE) { return castle(); }
		else if (me.unit == SPECS.CHURCH) { return church(); }
		else if (me.unit == SPECS.PILGRIM) { return pilgrim(); }
		else if (me.unit == SPECS.CRUSADER) { return crusader(); }
		else if (me.unit == SPECS.PROPHET) { return prophet(); }
		else if (me.unit == SPECS.PREACHER) { return preacher(); }
		return null;
	}

	public Action castle()
	{
	//	log(Arrays.deepToString(fullMap));
		return null;
	}


	public Action church()
	{
		return null;
	}


	public Action pilgrim()
	{
		return null;
	}


	public Action crusader()
	{
		return null;
	}


	public Action prophet()
	{
		return null;
	}


	public Action preacher()
	{
		return null;
	}


	public void getFMap()
	{
		boolean[][] m = getPassableMap();
		boolean[][] k = getKarboniteMap();
		boolean[][] f = getFuelMap();
		
		fullMap = new int[m.length][m[0].length];
		
		int h = m.length;
		int w = m[0].length;

		for(int i = 0; i < h; i++)
		{
			for(int j = 0; j < w; j++)
			{
				if(!m[i][j])
				{
					fullMap[i][j] = 1;
				}
				else if(k[i][j])
				{
					fullMap[i][j] = 2;
				}
				else if(f[i][j])
				{
					fullMap[i][j] = 3;
				}
				else
				{
					fullMap[i][j] = 0;
				}
			}
		}
	}

	public boolean getReflDir()
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
}