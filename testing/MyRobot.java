package bc19;

public class MyRobot extends BCAbstractRobot
{
	public Action turn()
	{
		log(me.x + " " + me.y);
		return null;
	}
}