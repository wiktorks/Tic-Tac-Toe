import javax.swing.JButton;

public class XOButton extends JButton 
{
	private String message;
	
	public XOButton(int i, int j)
	{
		super("");
		this.message = Integer.toString(i) + " " + Integer.toString(j);
	}
	
	public String getMessage()
	{
		return this.message;
	}
}
