package bc19;

import java.util.ArrayList;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	public Action turn() {
    	turn++;

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
    		if (turn == 1)
    		{
    			
    		}
    		return null;
    }
    
    /*
     * INPUT:
     * - int range1: the minimum range (squared) to look for karbonite
     * - int range2: the maximum range (squared) to look for karbonite
     * OUTPUT: an ArrayList that is
     * - empty if no such karbonite exists
     * - consisting of one set of { x,y } coordinates if one source of karbonite exists
     * - consisting of multiple sets of { x,y } coordinates if multiple sources of karbonite exists
     */
    private ArrayList<int[]> closestKarbInBounds(int range1, int range2)
    {
    		int min = range1;
    		int max = range2;
    		ArrayList<int[]> karbs = new ArrayList<int[]>();
    		while (min != max)
    		{
    			karbs = karbsInRange(min);
    			if (karbs.isEmpty())
    			{
    				min++;
    			}
    			else
    			{
    				return karbs;
    			}
    		}
    		return karbs;
    		
    }

    /*
     * INPUT:
     * - int range1: the minimum range (squared) to look for karbonite
     * - int range2: the maximum range (squared) to look for karbonite
     * OUTPUT: an ArrayList that is
     * - empty if no such karbonite exists
     * - consisting of one set of { x,y } coordinates if one source of karbonite exists
     * - consisting of multiple sets of { x,y } coordinates if multiple sources of karbonite exists
     */
    private ArrayList<int[]> closestFuelInBounds(int range1, int range2)
    {
    		int min = range1;
    		int max = range2;
    		ArrayList<int[]> fuels = new ArrayList<int[]>();
    		while (min != max)
    		{
    			fuels = fuelInRange(min);
    			if (fuels.isEmpty())
    			{
    				min++;
    			}
    			else
    			{
    				return fuels;
    			}
    		}
    		return fuels;
    		
    }
    
    /*
     * INPUT: 
     * - int range: Desired range^2
     * OUTPUT: a complete ArrayList<int[]> of all karbonite locations 
     * 		within the desired range of the robot in { x, y }
     */
    private ArrayList<int[]> karbsInRange(int range) 
    {
    		int rowInt = 0;
    		int colInt = 0;
    		ArrayList<int[]> karbs = new ArrayList<int[]>();
    		for (boolean[] row : karboniteMap)
    		{
    			for (boolean col : row)
    			{
    				if (isInRange(me.x, me.y, colInt, rowInt, range))
    				{
    					karbs.add(new int[] {colInt, rowInt});
    				}
    				colInt++;
    			}
    			rowInt++;
    			colInt = 0;
    		}
    		return karbs;
    }
    
    /*
     * INPUT: 
     * - int range: Desired range^2
     * OUTPUT: a complete ArrayList<int[]> of all fuel locations 
     * 		within the desired range of the robot in { x, y }
     */
    private ArrayList<int[]> fuelInRange(int range) 
    {
    		int rowInt = 0;
    		int colInt = 0;
    		ArrayList<int[]> fuels = new ArrayList<int[]>();
    		for (boolean[] row : fuelMap)
    		{
    			for (boolean col : row)
    			{
    				if (isInRange(me.x, me.y, colInt, rowInt, range))
    				{
    					fuels.add(new int[] {colInt, rowInt});
    				}
    				colInt++;
    			}
    			rowInt++;
    			colInt = 0;
    		}
    		return fuels;
    }
    
    /*INPUT:
     * - int x1, y1: the x and y coordinates of the first space
     * - int x2, y2: the x and y coordinates of the second space
     * - int range: the maximum distance (squared) that the two spaces can be apart in order to return true
     * OUTPUT: true if the distance squared between the two coordinates are equal to or less than range
     */
    private boolean isInRange(int x1, int y1, int x2, int y2, int range)
    {
    		return range >= Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2);
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
}