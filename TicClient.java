import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class TicClient implements ActionListener
{
	private XOButton tab[][];
	private volatile boolean myTurn ;
	private Socket sock;
	private BufferedReader input;
	private volatile PrintStream output;
	private volatile int wygrana;
	private volatile int counter;
	private JFrame frame;
	private JMenuItem newGame;
	private JMenuItem save; 
	private JMenuItem load; 
	private final String url = "jdbc:hsqldb:file:saveClientGames";
	private Connection connection = null;
	
	public TicClient() throws UnknownHostException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		myTurn = true;
		sock = new Socket("127.0.0.1", 9001);
		input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		output = new PrintStream(sock.getOutputStream(), true);
		tab = new XOButton[20][20];
		wygrana = 0;
		counter = 0;
		Class.forName("org.hsqldb.jdbcDriver").newInstance();
	}
	
	public void createGUI()
	{
		frame = new JFrame("TicTacToe - Klient");
		JPanel mainPanel = new JPanel();
		JPanel gamePanel = new JPanel();
		JPanel optionPanel = new JPanel();
		JLabel label = new JLabel("Grasz Kó³kami!");
		JMenuBar menuBar = new JMenuBar();
		JMenu mainMenu = new JMenu("Opcje");
		newGame = new JMenuItem("Nowa gra");
		newGame.addActionListener(this);
		save = new JMenuItem("Zapisz grê"); 
		save.addActionListener(this);
		load = new JMenuItem("Wczytaj grê");
		load.addActionListener(this);
		
		mainMenu.add(newGame);
		mainMenu.addSeparator();
		mainMenu.add(save);
		mainMenu.addSeparator();
		mainMenu.add(load);
		menuBar.add(mainMenu);
		frame.setJMenuBar(menuBar);
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		optionPanel.setLayout(new FlowLayout());
		optionPanel.add(label);
		gamePanel.setLayout(new GridLayout(20, 20));
		gamePanel.setPreferredSize(new Dimension(700, 700));
		for(int i=0; i<20; i++)
		{
			for(int j=0; j<20; j++)
			{
				tab[i][j] = new XOButton(i, j);
				tab[i][j].setFont(new Font(tab[i][j].getFont().getName(), tab[i][j].getFont().getStyle(), 30));
				tab[i][j].setMargin(new Insets(0, -1, 0, 0));
				tab[i][j].setActionCommand("B");
				tab[i][j].addActionListener(this);
				gamePanel.add(tab[i][j]);
			}
		}
		
		mainPanel.add(optionPanel);
		mainPanel.add(gamePanel);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(750, 800));
		frame.setResizable(false);
		frame.add(mainPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getActionCommand().equals("B"))
		{
			if(!myTurn)
				return;
			
			XOButton b = (XOButton) e.getSource();
			if(!b.getText().equals("X") && !b.getText().equals("O"))
			{
				b.setForeground(Color.BLUE);
				b.setText("O");
				if(this.checkIfWin(b.getMessage()))
				{
					output.println("WYGRANA " + b.getMessage());
					wygrana = 1;
				}
				else
				{
					output.println(b.getMessage());
				}
				counter++;
				myTurn = false;
			}
		}
		
		if(e.getActionCommand().equals("Nowa gra"))
		{
			try 
			{
				this.restart();
			} 
			catch (IOException e1) 
			{
				e1.printStackTrace();
			}
		}
		
		if(e.getActionCommand().equals("Zapisz grê")) //Zadanie 7
		{
			try 
			{
				saveGame();
			} catch (SQLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if(e.getActionCommand().equals("Wczytaj grê")) //Zadanie 7
		{
			try 
			{
				loadGame();
			} catch (SQLException e1) 
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	public void saveGame() throws SQLException 
	{
		connection = DriverManager.getConnection(url, "username", "password");
		Statement st = connection.createStatement();
		st.executeUpdate("DROP TABLE IF EXISTS SAVE");
		st.executeUpdate("CREATE TABLE SAVE (Nazwa CHAR(1), x INTEGER, y INTEGER)");
		
		for(int i=0; i<20; i++)
		{
			for(int j=0; j<20; j++)
			{
				if(!tab[i][j].getText().equals(""))
				{
					String statement = "INSERT INTO SAVE (Nazwa, x, y) VALUES ('" + tab[i][j].getText() + "', " + Integer.toString(i) + ", " + Integer.toString(j) + ")";
					st.executeUpdate(statement);
				}
			}
		}
		st.close();
		connection.close();
	}
	
	public void loadGame() throws SQLException
	{
		connection = DriverManager.getConnection(url, "username", "password");
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery("SELECT * FROM SAVE");
		for(int i=0; i<20; i++)
		{
			for(int j=0; j<20; j++)
			{
				tab[i][j].setText("");
			}
		}
		int isMyTurn = 0;
		while(rs.next())
		{
			int x = rs.getInt("x");
			int y = rs.getInt("y");
			String name = rs.getString("Nazwa");
			if(name.equals("O"))
				tab[x][y].setForeground(Color.BLUE);
			else
				tab[x][y].setForeground(Color.RED);
			
			tab[x][y].setText(name);
			isMyTurn++;
		}
		if(isMyTurn % 2 == 0)
			this.myTurn = true;
		else
			this.myTurn = false;
		
		st.close();
		connection.close();
	}
	
	public boolean checkIfWin(String position)
	{
		String [] s = position.split(" ");
		int x = Integer.parseInt(s[0]);
		int y = Integer.parseInt(s[1]);
		int size =0;
		int size_pion = 0;
		int skos_1 =0;
		int skos_2 =0;
		
		if(tab[x][y].getText().equals("O"))
		{
			size++;
			size_pion++;
			skos_1++;
			skos_2++;
			
			/*w prawo*/
			int temp = x+1;
			while(temp<20)
			{
				if(tab[temp][y].getText().equals("O"))
				{
					size++;
					temp++;
				}
				else break;
			}
			
			/*w lewo*/
			temp = x-1;
			while(temp>-1)
			{
				if(tab[temp][y].getText().equals("O"))
				{
					size++;
					temp--;
				}
				else break;
			}
			
			/*w gore*/
			temp = y-1;
			while(temp>-1)
			{
				if(tab[x][temp].getText().equals("O"))
				{
					size_pion++;
					temp--;
				}
				else break;
			}
			
			/*w dol*/
			temp = y+1;
			while(temp<20)
			{
				if(tab[x][temp].getText().equals("O"))
				{
					size_pion++;
					temp++;
				}
				else break;
			}
			
			int temp_x = x+1;
			int temp_y = y+1;
			while(temp_x<20&temp_y<20)
			{
				if(tab[temp_x][temp_y].getText().equals("O"))
				{
					skos_1++;
					temp_x++;
					temp_y++;
				}
				else break;
			}
			 
			temp_x = x-1;
			temp_y = y-1;
			while(temp_x>-1&temp_y>-1)
			{
				if(tab[temp_x][temp_y].getText().equals("O"))
				{
					skos_1++;
					temp_x--;
					temp_y--;
				}
				else break;
			}
			
			temp_x = x+1;
			temp_y = y-1;
			while(temp_x<20&temp_y>-1)
			{
				if(tab[temp_x][temp_y].getText().equals("O"))
				{
					skos_2++;
					temp_x++;
					temp_y--;
				}
				else break;
			}
			
			temp_x = x-1;
			temp_y = y+1;
			while(temp_x>-1&temp_y<20)
			{
				if(tab[temp_x][temp_y].getText().equals("O"))
				{
					skos_2++;
					temp_x--;
					temp_y++;
				}
				else break;
			}
		}
		
		if(size>=5 || size_pion>=5 || skos_1>=5 || skos_2>=5)
			return true;
			
		return false;
	}
	
	public boolean endGame() throws IOException
	{
		String komunikat = "Remis!";
		if(wygrana == -1)
			komunikat = "Przegrales!";
		if(wygrana == 1)
			komunikat = "Wygrales!";
		
		Object[] options = {"Nowa Gra", "Wyjdz"};
		int wybor = JOptionPane.showOptionDialog(frame, komunikat, "TicTacToe",
	            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
	            null, options, options[1]);
		if(wybor == 0)
		{
			restart();
			return false;
		}
		else
		{
			frame.dispose();
			return true;
		}
	}
	
	public void restart() throws IOException
	{
		for(int i=0; i<20; i++)
		{
			for(int j=0; j<20; j++)
			{
				tab[i][j].setText("");
			}
		}
		myTurn = true;
		counter = 0;
		wygrana = 0;
		input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		output = new PrintStream(sock.getOutputStream(), true);
	}
	
	public void run() throws IOException
	{
		createGUI();
		while(true)
		{
			if(wygrana != 0 || counter == 400)
				if(endGame())
					break;
			
			if(myTurn == false)
			{
				String out = input.readLine();
				myTurn = true;
				if(out != null)
				{
					if(out.startsWith("W"))
					{
						String[] s = out.split(" ");
						XOButton b = tab[Integer.parseInt(s[1])][Integer.parseInt(s[2])];
						b.setForeground(Color.RED);
						b.setText("X");
						wygrana = -1;
					}
					else
					{
						String[] s = out.split(" ");
						XOButton b = tab[Integer.parseInt(s[0])][Integer.parseInt(s[1])];
						b.setForeground(Color.RED);
						b.setText("X");
					}
					counter++;
				}
			}
		}
	}

	private void close() throws IOException, SQLException
	{
		this.sock.close();
		this.input.close();
		this.output.close();
	}
	
	public static void main(String args[]) 
	{
		TicClient ts = null;
		try
		{
			ts = new TicClient();
			ts.run();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				ts.close();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}