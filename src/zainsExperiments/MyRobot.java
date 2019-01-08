package src.zainsExperiments;

import src.library.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

    public Action turn() {
    	turn++;

    	if (me.unit == SPECS.CASTLE)
    	{
    		if(karbonite == 5) {}
    	}
   
    	
    	else if (me.unit == SPECS.CHURCH)
    	{

    	}
    	
    	
    	else if (me.unit == SPECS.PILGRIM)
    	{
    		
    	}
    	
    	
    	else if (me.unit == SPECS.CRUSADER)
    	{
    		
    	}
    	
    	
    	else if (me.unit == SPECS.PROPHET)
    	{
    		
    	}
    	
    	
    	else if (me.unit == SPECS.PREACHER)
    	{
    		
    	}
    	
    	
    	return null;

	}
}