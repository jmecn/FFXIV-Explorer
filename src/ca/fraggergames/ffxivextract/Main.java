package ca.fraggergames.ffxivextract;

import java.io.IOException;

import javax.swing.UIManager;

import ca.fraggergames.ffxivextract.gui.FileManagerWindow;
import ca.fraggergames.ffxivextract.helpers.LERandomAccessFile;
import ca.fraggergames.ffxivextract.helpers.LuaDec;
import ca.fraggergames.ffxivextract.helpers.PathHashList;
import ca.fraggergames.ffxivextract.models.Log_File;
import ca.fraggergames.ffxivextract.models.SCD_File;

public class Main {

	public static void main(String[] args) {		
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		FileManagerWindow fileMan = new FileManagerWindow(Constants.APPNAME);
		fileMan.setVisible(true);						
		
		try {
			Constants.hashDatabase = PathHashList.loadKryo("C:\\Users\\Filip\\Desktop\\filelist2.db");			
			System.out.println("Loaded: " + Constants.hashDatabase.getNumFiles() + " files and " + Constants.hashDatabase.getNumFolders() + " folders.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
}
