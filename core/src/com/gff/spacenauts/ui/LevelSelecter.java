package com.gff.spacenauts.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.gff.spacenauts.AssetsPaths;
import com.gff.spacenauts.Globals;
import com.gff.spacenauts.Spacenauts;
import com.gff.spacenauts.net.NetworkAdapter;
import com.gff.spacenauts.screens.GameScreen;
import com.gff.spacenauts.screens.InitialScreen;
import com.gff.spacenauts.screens.LoadingScreen;
import com.gff.spacenauts.screens.NarrativeScreen;

/**
 * An UI element that allows the player to choose a new level. It's made by a big preview, two arrows for going to the next or previous preview,
 * and a START button for playing the selected level.
 * 
 * @author Alessio Cali'
 *
 */
public class LevelSelecter implements UISet {

	/**
	 * An enumeration that includes all levels shown by the Selecter, including a reference name for the preview picture, the map of said level,
	 * a label string and an optional introductory cutscene. 
	 * 
	 * @author Alessio Cali'
	 *
	 */
	public enum LevelSelectSet {

		TUTORIAL("tutorial", "tutorial.tmx", "Tutorial", "intro"),
		LEVEL_1("level1", "level1.tmx", "Level_1", null),
		LEVEL_2("level2", "level2.tmx", "Level_2", "level2_intro"),
		LEVEL_3("level3", "level3.tmx", "Level_3", "level3_intro");

		private String preview;
		private String map;
		private String name;
		private String cutscene;

		private static LevelSelectSet[] values = LevelSelectSet.values();

		private LevelSelectSet(String preview, String map, String name, String cutscene) {
			this.preview = preview;
			this.map = "maps/" + map;
			this.name = name;
			this.cutscene = cutscene;
		}

		/**
		 * Tells whether the given map file has a corresponding, set.
		 * 
		 * @param map the path of the given map file.
		 * @return if that map exists as a LevelSet
		 */
		public static boolean containsMap(String map) {
			return LevelSelectSet.forMap(map) != null;
		}

		/**
		 * Gets the LevelSelectSet corresponding to the given map file.
		 * 
		 * @param map the map file path
		 * @return
		 */
		public static LevelSelectSet forMap(String map) {
			for (LevelSelectSet set : values) 
				if (map.equals(set.getMapString())) return set;
		
			return null;
		}

		public String getPreviewString() {
			return preview;
		}

		public String getMapString() {
			return map;
		}
		
		public String getName() {
			return name;
		}

		/**
		 * Returns the position of this set within the values array.
		 * 
		 * @return The index of this set, or -1 if not found.
		 */
		public int getPosition() {
			int len = values.length;
			int found = -1;

			for (int i  = 0 ; i < len ; i++) {
				if (values[i] == this) {
					found = i;
					break;
				}
			}

			return found;		
		}

		/**
		 * Tells whether this level has been unlocked.
		 * 
		 * @return
		 */
		public boolean isUnlocked () {
			return this.getPosition() <= Globals.levelUnlocked;
		}
		
		public String getCutscene () {
			return cutscene;
		}

		public LevelSelectSet getNext() {
			int len = values.length;
			int index = getPosition();

			//No successor if last (len -1), not found (-1) or not unlocked.
			if (index == len - 1 || index == -1 || index >= Globals.levelUnlocked)
				return null;
			else
				return values[index + 1];
		}

		public LevelSelectSet getPrevious() {
			int index = getPosition();

			if (index == 0 || index == -1)
				return null;
			else
				return values[index - 1];
		}
		
	}

	/**
	 * A simple counter that goes from 60 to 0. Once it expires
	 * it starts the game with a default value. Used in multiplayer
	 * mode to not hang too much... Not that it has much use as of now.
	 * 
	 * @author Alessio
	 *
	 */
	private class CountDownLabel extends Label {

		private float countDown = 60;
		private boolean expired = false;

		public CountDownLabel(String text, Label.LabelStyle style) {
			super(text, style);
		}

		@Override
		public void act(float delta) {
			super.act(delta);
			
			if (!expired) {
				countDown = countDown > delta ? countDown - delta : 0;

				setText(String.valueOf(MathUtils.ceil(countDown)));

				if (countDown <= 0) {
					start(LevelSelectSet.LEVEL_1);
					expired = true;
				}
			}
		}

		public void reset() {
			countDown = 60;
			expired = false;
		}

		public void halt() {
			expired = true;
		}
	}

	private LevelSelectSet current;
	private TextureAtlas uiAtlas;
	private TextureAtlas previewAtlas;
	private Game game;
	private InitialScreen initial;
	private UISet from;

	private Table mainTable;
	private Image logo;
	private ImageButton backButton;
	private CountDownLabel countDownLabel;
	private Label levelLabel;
	private ImageButton arrowPrevious;
	private Image preview;
	private ImageButton arrowNext;
	private Label startLabel;

	private final boolean multiplayer;

	public LevelSelecter(final Game game, AssetManager assets, final InitialScreen initial, final UISet from) {
		this(game, assets, initial, from, false);
	}

	public LevelSelecter(final Game game, AssetManager assets, final InitialScreen initial, final UISet from, final boolean multiplayer) {
		this.game = game;
		this.multiplayer = multiplayer;
		this.initial = initial;
		this.from = from;
		
		//Assets
		uiAtlas = assets.get(AssetsPaths.ATLAS_UI);
		previewAtlas = assets.get(AssetsPaths.ATLAS_PREVIEWS);
		BitmapFont k64 = assets.get(AssetsPaths.FONT_KARMATIC_64);
		
		//Styles
		Label.LabelStyle style = new Label.LabelStyle(k64, Color.WHITE);
		
		//UISet elements
		mainTable = new Table();
		
		logo = new Image(new TextureRegionDrawable(uiAtlas.findRegion("selectlevel_logo")));
		
		backButton = new ImageButton(new TextureRegionDrawable(uiAtlas.findRegion("back_button")));
		backButton.addListener(new ClickListener(){
			@Override
			public void clicked(InputEvent e, float x, float y) {
				initial.setUI(from);
				if (multiplayer && Spacenauts.getNetworkAdapter() != null) { 
					Spacenauts.getNetworkAdapter().reset(); 
				}
				if (countDownLabel != null) { 
					countDownLabel.reset();
				}
			}
		});
		
		countDownLabel = multiplayer ? new CountDownLabel("", style) : null;

		//The level title
		levelLabel = new Label("", style);
		mainTable.add(levelLabel).center().colspan(3).row();		

		//The "previous" arrow
		arrowPrevious = new ImageButton(new TextureRegionDrawable(uiAtlas.findRegion("left_arrow")));
		arrowPrevious.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				setSet(current.getPrevious());
			}
		});
		mainTable.add(arrowPrevious).center();

		//The preview image
		preview = new Image();
		mainTable.add(preview).expand();

		//The "next" arrow
		arrowNext = new ImageButton(new TextureRegionDrawable(uiAtlas.findRegion("right_arrow")));
		arrowNext.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				setSet(current.getNext());
			}
		});
		mainTable.add(arrowNext).center().row();

		//The start button
		startLabel = new Label("Start", style);
		startLabel.addListener(new ClickListener() { 
			@Override
			public void clicked(InputEvent event, float x, float y) {
				start(current);
			}
		});
		mainTable.add(startLabel).center().colspan(3);

		//Apply the Tutorial level set.
		setSet(LevelSelectSet.TUTORIAL);
	}

	/**
	 * Applies the given LevelSelectSet, showing the correct preview,
	 * level name, and updating the current set.
	 * 
	 * @param set
	 */
	private void setSet(LevelSelectSet set) {
		if (set == null) return;	//Do nothing if set is null

		current = set;

		if (set.getPrevious() == null) arrowPrevious.setVisible(false);
		else arrowPrevious.setVisible(true);

		if (set.getNext() == null) arrowNext.setVisible(false);
		else arrowNext.setVisible(true);

		levelLabel.setText(current.getName());
		preview.setDrawable(new TextureRegionDrawable(previewAtlas.findRegion(current.getPreviewString())));
	}

	/**
	 * If on multiplayer, sends a registration request to the {@link NetworkAdapter}.
	 * Otherwise starts a new game with the given LevelSelectSet.
	 * 
	 * @param levelSet
	 */
	private void start(LevelSelectSet levelSet) {
		if (multiplayer) {
			Spacenauts.getNetworkAdapter().register(Globals.nickname, Globals.timeout, levelSet.name + " " + levelSet.map);
			countDownLabel.halt();
			countDownLabel.reset();
			initial.setUI(from);
		} 
		
		else {
			GameScreen gameScreen = new GameScreen(levelSet.map, game); 
			LoadingScreen loadingScreen = new LoadingScreen(gameScreen, game, gameScreen); 
			
			//There's a cutscene, set a NarrativeScreen first
			if (levelSet.getCutscene() != null) 
				game.setScreen(new NarrativeScreen(levelSet.cutscene, loadingScreen, game));
			
			 else
				game.setScreen(loadingScreen);
		}
	}
	
	public void refresh () {
		if (current != null) 
			setSet(current);
		else
			setSet(LevelSelectSet.TUTORIAL);
	}

	@Override
	public Image logo() {
		return logo;
	}

	@Override
	public ImageButton lowerLeft () {
		return backButton;
	}

	@Override
	public Label lowerRight () {
		return countDownLabel;
	}

	@Override
	public Table main () {
		return mainTable;
	}
}
