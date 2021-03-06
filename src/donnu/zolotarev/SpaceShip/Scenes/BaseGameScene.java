package donnu.zolotarev.SpaceShip.Scenes;

import android.graphics.Point;
import android.opengl.GLES20;
import android.util.Log;
import android.view.KeyEvent;
import donnu.zolotarev.SpaceShip.Activity.GameActivity;
import donnu.zolotarev.SpaceShip.Bullets.BaseBullet;
import donnu.zolotarev.SpaceShip.Effects.Boom;
import donnu.zolotarev.SpaceShip.Effects.Fog;
import donnu.zolotarev.SpaceShip.Effects.FogManager;
import donnu.zolotarev.SpaceShip.Effects.Shield;
import donnu.zolotarev.SpaceShip.GameData.HeroFeatures;
import donnu.zolotarev.SpaceShip.GameData.Settings;
import donnu.zolotarev.SpaceShip.GameState.IHeroDieListener;
import donnu.zolotarev.SpaceShip.GameState.IParentScene;
import donnu.zolotarev.SpaceShip.GameState.IWaveBar;
import donnu.zolotarev.SpaceShip.R;
import donnu.zolotarev.SpaceShip.Scenes.Interfaces.ISimpleClick;
import donnu.zolotarev.SpaceShip.Textures.MusicLoader;
import donnu.zolotarev.SpaceShip.Textures.TextureLoader;
import donnu.zolotarev.SpaceShip.UI.FileMode;
import donnu.zolotarev.SpaceShip.UI.IHealthBar;
import donnu.zolotarev.SpaceShip.UI.IScoreBar;
import donnu.zolotarev.SpaceShip.Units.BaseUnit;
import donnu.zolotarev.SpaceShip.Units.EnemyBoss;
import donnu.zolotarev.SpaceShip.Units.Hero;
import donnu.zolotarev.SpaceShip.Utils.*;
import donnu.zolotarev.SpaceShip.Waves.IAddedEnemy;
import donnu.zolotarev.SpaceShip.Waves.IWaveController;
import org.andengine.engine.Engine;
import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground;
import org.andengine.entity.scene.menu.MenuScene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSCounter;
import org.andengine.input.touch.TouchEvent;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.color.Color;

import java.util.Random;

public abstract class BaseGameScene extends MyScene implements IAddedEnemy, IScoreBar {

    protected final Settings setting;
    protected Color textColor = Color.WHITE;

    protected static BaseGameScene activeScene;
    protected static Engine engine;
    protected final GameActivity shipActivity;
    protected IWaveController waveController;
    private final ObjectCollisionController enemyController;
    private final ObjectCollisionController bulletController;
    private final IParentScene parentScene;
    private HUD analogOnScreenControl;

    protected Hero hero;
    private Text waveCountBar;
    private Text scoreBar;
    private Text healthBar;
    private Text shueldBar;

    private MenuScene menuScene;
    private MenuScene dieMenuScene;
    private boolean isShowMenuScene = false;
    protected boolean enablePauseMenu = true;
    protected boolean isActive = false;

    protected int score;
    protected int waveIndex = 0;


    protected IHealthBar textHealthBarCallback = new IHealthBar() {
        @Override
        public void updateHealthBar(int health) {
            healthBar.setText(String.valueOf(health));
        }

        @Override
        public void updateShueldBar(int health) {
            shueldBar.setText(String.valueOf(health));
        }
    };

    protected IWaveBar textWaveBarCallback = new IWaveBar() {
        @Override
        public void onNextWave(int count) {
            waveCountBar.setText(String.format("%01d", count));
        }
    };
    private int status;
    private ISimpleClick exit;
    private ISimpleClick restart;
    private Text rocketBar;

    private IHeroDieListener heroDieListener =  new IHeroDieListener() {
        @Override
        public void heroDie() {
            status = IParentScene.EXIT_DIE;
            beforeReturnToParent(IParentScene.EXIT_DIE);
            String str = shipActivity.getString(R.string.text_die_money,score);
            dieMenuScene = MenuFactory.createMenu(engine,shipActivity.getCamera(), shipActivity)
                    .addedText(shipActivity.getString(R.string.lose_text),TextureLoader.getFont(),
                            Constants.CAMERA_WIDTH_HALF,100, WALIGMENT.CENTER, HALIGMENT.CENTER)
                    .addedText(str,TextureLoader.getFont())
                    .addedItem(TextureLoader.getMenuButtonBackgroundTextureRegion(), R.string.btn_game_restart,
                            TextureLoader.getFont(), new ISimpleClick() {
                        @Override
                        public void onClick(int id) {

                            restart.onClick(id);
                        }
                    }/*restart*/)
                    .addedItem(TextureLoader.getMenuButtonBackgroundTextureRegion(),
                            R.string.btn_game_back_to_main_menu, TextureLoader.getFont(), exit)
                    .enableAnimation()
                    .build();

            shipActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isActive = false;
                    enablePauseMenu = false;
                    setChildScene(dieMenuScene, false, true, true);
                }
            });

        }
    };


    public BaseGameScene(IParentScene self) {
        super(self);
        setting = Settings.get();

        if (setting.isMusic() && !MusicLoader.getSound().isPlaying()){
            MusicLoader.getSound().play();
        }

        parentScene = self;
        activeScene = this;
        shipActivity = GameActivity.getInstance();
        engine = shipActivity.getEngine();
        final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 12);
        autoParallaxBackground.setColor(Color.BLACK);
        Sprite background =  new Sprite(0,0,TextureLoader.getGameBK(),shipActivity.getVertexBufferObjectManager());
        autoParallaxBackground.attachParallaxEntity(new ParallaxBackground.ParallaxEntity(-20.0f, background));
        setBackground(autoParallaxBackground);

        enemyController = new ObjectCollisionController<BaseUnit>();
        bulletController = new ObjectCollisionController<BaseBullet>();

        //createFPSBase();
        createHealthBar();
        createScoreBar();
        createWaveCountBar();
        initUnitsPools();
        initHero();
        initBulletsPools();
        addHeroMoveControl();

        BaseBullet.setDieListener(heroDieListener);
        BaseUnit.setDieListener(heroDieListener);
        initWave();
        createMenu();
        status = IParentScene.EXIT_USER;

    }


    protected abstract void initWave();

    protected abstract void initHero();

    protected abstract void initBulletsPools();

    protected abstract void initUnitsPools();

    protected abstract void beforeReturnToParent(int status);

    private void createMenu() {

        restart = new ISimpleClick() {
            @Override
            public void onClick(int id) {
                clearItem();
                parentScene.restart(status);
            }
        };

        exit = new ISimpleClick() {
            @Override
            public void onClick(int id) {
                returnToParentScene(status);
            }
        };

        menuScene = MenuFactory.createMenu(engine, shipActivity.getCamera(), shipActivity)
                .addedItem(TextureLoader.getMenuButtonBackgroundTextureRegion(),R.string.btn_game_resume, TextureLoader.getFont(), new ISimpleClick() {
                            @Override
                            public void onClick(int id) {
                                activeScene.detachChild(menuScene);
                                activeScene.setChildScene(analogOnScreenControl);
                                isShowMenuScene = false;
                                isActive = true;
                            }
                })
                .addedItem(TextureLoader.getMenuButtonBackgroundTextureRegion(), R.string.btn_game_restart, TextureLoader.getFont(), restart)
                .addedItem(TextureLoader.getMenuButtonBackgroundTextureRegion(), R.string.btn_game_back_to_main_menu, TextureLoader.getFont(), exit)
                .enableAnimation()
                .build();
    }


    boolean flag = false;

    protected void addHeroMoveControl() {
        AnalogOnScreenControl hud = null;
        if (setting.isAnalogflyControlMode()){
            hud = new AnalogOnScreenControl(70,
                    GameActivity.getCameraHeight() - TextureLoader.getScreenControlBaseTextureRegion().getHeight() - 50,
                    shipActivity.getCamera(), TextureLoader.getScreenControlBaseTextureRegion(),
                    TextureLoader.getScreenControlKnobTextureRegion(), 0.1f, 100,
                    shipActivity.getEngine().getVertexBufferObjectManager(),
                    hero.getCallback());
            hud.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            hud.getControlBase().setAlpha(0.5f);
            hud.getControlBase().setScaleCenter(0, 128);
            hud.getControlBase().setScale(1.5f);
            hud.getControlKnob().setScale(1.5f);
            hud.refreshControlKnobPosition();
            analogOnScreenControl = hud;
        }else{
            analogOnScreenControl = new HUD();
            analogOnScreenControl.setCamera(shipActivity.getCamera());
        }


        setChildScene(analogOnScreenControl);
        analogOnScreenControl.setZIndex(1000);


        setOnSceneTouchListener(new IOnSceneTouchListener() {
            @Override
            public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
                if (!setting.isAnalogflyControlMode()){
                    if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN || pSceneTouchEvent.getAction() == TouchEvent.ACTION_MOVE){
                        hero.flyTo(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
                    }
                }
                if (setting.getFileMode() == FileMode.BY_HOLD){
                    hero.canFire(flag);
                    flag = false;
                }
                return false;
            }
        });


         int i = 0;
        if (setting.getFileMode() == FileMode.BY_HOLD){
           createFireButton();
            i = 300;
        } else {
            hero.canFire(true);
            i = 130;
        }


        final HeroFeatures heroFeatures = HeroFeatures.get();
        if (heroFeatures.isHaveRocketGun()){


        final Sprite btnFire2 = new Sprite(GameActivity.getCameraWidth()- i, GameActivity.getCameraHeight()-150,
                TextureLoader.getBtnFire2(),shipActivity.getEngine().getVertexBufferObjectManager()){
            boolean flag = false;
            public int pId = -1;
            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {

                if (heroFeatures.isHaveRocket()){
                    shipActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(pSceneTouchEvent.isActionDown()){
                                hero.fireRocket();
                                rocketBar.setText(String.valueOf(heroFeatures.useRocket()));

                            }
                        }
                    });
                }

                return true;
            }
        };

       // btnFire2.setAlpha(0.5f);

            btnFire2.setScale(1.25f);
            analogOnScreenControl.attachChild(btnFire2);
            analogOnScreenControl.registerTouchArea(btnFire2);
            createRocketBar(i);
            rocketBar.setText(String.valueOf(heroFeatures.getRocketCount()));
        }
    }



    private void createFireButton() {
        final Sprite btnFire = new Sprite(GameActivity.getCameraWidth()-130, GameActivity.getCameraHeight()-150,
                TextureLoader.getBtnFire1(),shipActivity.getEngine().getVertexBufferObjectManager()){

            public int pId = -1;

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {

              /*  shipActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {*/

                if(pSceneTouchEvent.isActionDown() || pSceneTouchEvent.isActionMove()){
                    hero.canFire(true);
                    flag = true;
                }
                if(pSceneTouchEvent.isActionUp() || pSceneTouchEvent.isActionOutside() || pSceneTouchEvent.isActionCancel()){
                //    if ( Constants.CAMERA_WIDTH_HALF < pSceneTouchEvent.getX()){
                        flag = false;
                        hero.canFire(false);
                  //  }

                }


                //   }
                // });

                return true;
            }
        };

       /* btnFire.setColor(Color.BLACK);
        btnFire.setAlpha(0.5f);*/

        analogOnScreenControl.attachChild(btnFire);
        analogOnScreenControl.registerTouchArea(btnFire);
        btnFire.setScale(1.25f);


}

    private void createRocketBar(int i){
        try {
            int x = GameActivity.getCameraWidth()- i+65;
            int y = GameActivity.getCameraHeight()- 85;
            rocketBar = new Text(x,y,TextureLoader.getFont(),"00",new TextOptions(HorizontalAlign.LEFT),engine.getVertexBufferObjectManager());
            rocketBar.setText("0");
            analogOnScreenControl.attachChild(rocketBar);
            analogOnScreenControl.setZIndex(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        if (isActive){
            waveController.updateWave(pSecondsElapsed);
            FogManager.fogUpdate();
        }
        super.onManagedUpdate(pSecondsElapsed);
    }

    public void onKeyPressed(int keyCode, KeyEvent event) {
        if (enablePauseMenu){
            if (!isShowMenuScene){
                isShowMenuScene = true;
                isActive = false;
                setChildScene(menuScene, false, true, true);
            }else{
                isActive = true;
                isShowMenuScene = false;
                activeScene.detachChild(menuScene);
                activeScene.setChildScene(analogOnScreenControl);
            }
        }
    }

    protected void returnToParentScene(int statusCode){
        MusicLoader.getSound().stop();
        beforeReturnToParent(statusCode);
        clearItem();
        parentScene.returnToParentScene(statusCode);
    }

    protected void clearItem(){
        hero.destroy(false);

        analogOnScreenControl.detachChildren();
        Shield.bulletsPool();
        FogManager.fogOff();
        Boom.clear();
        FogManager.fogOff();
        Fog.clearClass();
        getEnemyController().cleer();
        getBulletController().cleer();
        BaseBullet.clearClass();
        BaseUnit.clearClass();

        hero = null;
        activeScene = null;
        engine = null;
        waveController = null;
        detachChildren();
        clearUpdateHandlers();
        clearChildScene();
        clearEntityModifiers();
        clearTouchAreas();
        detachSelf();
        System.gc();
    }

    public void addToScore(int value){
        score += value;
        scoreBar.setText(String.format("%08d", score));
    }

    private void createHealthBar(){
        try {
            int y = 0;
            int x = 0;// (int)TextureLoader.getScreenControlBaseTextureRegion().getWidth();
            String str = shipActivity.getString(R.string.text_strength);
            Text text = new Text(x,y,TextureLoader.getFont(),str,new TextOptions(HorizontalAlign.LEFT),engine.getVertexBufferObjectManager());
            attachChild(text);
            text.setZIndex(1000);
            x += text.getWidth();
            healthBar = new Text(x,y,TextureLoader.getFont(),"123456",new TextOptions(HorizontalAlign.LEFT),engine.getVertexBufferObjectManager());
            healthBar.setColor(textColor);
            attachChild(healthBar);
            healthBar.setZIndex(1000);
            final HeroFeatures heroFeatures = HeroFeatures.get();

            x += healthBar.getWidth()+5;
            str = shipActivity.getString(R.string.text_shield);

            text = new Text(x,y,TextureLoader.getFont(),str,new TextOptions(HorizontalAlign.LEFT),engine.getVertexBufferObjectManager());
            attachChild(text);
            x += text.getWidth();
            text.setZIndex(1000);
            shueldBar = new Text(x,y,TextureLoader.getFont(),"12345",new TextOptions(HorizontalAlign.LEFT),engine.getVertexBufferObjectManager());
            shueldBar.setColor(textColor);
            attachChild(shueldBar);
            shueldBar.setZIndex(1000);

           boolean useS =  heroFeatures.isHaveShield() && heroFeatures.isNeedUseShield();
            text.setVisible(useS);
            shueldBar.setVisible(useS);
            //shueldBar

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createFPSBase() {
        final FPSCounter fpsCounter = new FPSCounter();
        engine.registerUpdateHandler(fpsCounter);
        final Text fpsText = new Text(0, 0, TextureLoader.getFont(), "FPS:", "FPS: 1234567890.".length(),engine.getVertexBufferObjectManager());
        fpsText.setColor(textColor);
        attachChild(fpsText);
        fpsText.setZIndex(1000);
        registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {

                fpsText.setText("FPS: " + String.valueOf(fpsCounter.getFPS()));
            }
        }));
    }

    private void createWaveCountBar() {
        try {
            waveCountBar = new Text(0,0, TextureLoader.getFont(),"000",new TextOptions(HorizontalAlign.RIGHT),engine.getVertexBufferObjectManager());
            int x = GameActivity.getCameraWidth() - (int)scoreBar.getWidth() - (int)scoreBar.getWidth() -20;
            waveCountBar.setPosition(x,0);
            waveCountBar.setColor(textColor);
            waveCountBar.setText("0");
            attachChild(waveCountBar);
            waveCountBar.setZIndex(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createScoreBar() {
        try {
            scoreBar = new Text(0,0,TextureLoader.getFont(),"000000000",new TextOptions(HorizontalAlign.RIGHT),engine.getVertexBufferObjectManager());
            int x = GameActivity.getCameraWidth() - (int)scoreBar.getWidth();
            scoreBar.setPosition(x,0);
            scoreBar.setColor(textColor);
            attachChild(scoreBar);
            scoreBar.setZIndex(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Hero getHero() {
        return hero;
    }

    public ObjectCollisionController getEnemyController() {
        return enemyController;
    }

    public ObjectCollisionController getBulletController() {
        return bulletController;
    }

    public static Engine getEngine() {
        return engine;
    }

    public static BaseGameScene getActiveScene() {
        return activeScene;
    }

    protected void start(){
        isActive = true;
    }

    public void addNewWaveController(IWaveController controller){
        waveController = controller;
        start();
    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {
        System.gc();
        Log.i("XXX", "isShowMenuScene =  " + (isShowMenuScene));
        if (enablePauseMenu){
            if (!isShowMenuScene){
                isShowMenuScene = true;
                isActive = false;
                setChildScene(menuScene, false, true, true);
            }
        }
    }

    @Override
    public boolean addEnemy(AddedEnemyParam param) {
        if (param.isEnemy()){
            return true;
        }else{
            switch (param.getKind()){
                case FogManager.START_FOG:
                    FogManager.fogOn();
                    break;
                case FogManager.STOP_FOG:
                    FogManager.fogOff();
                    break;
                case BaseUnit.TYPE_ENEMY_BOSS_1:
                    EnemyBoss enemyBoss = new EnemyBoss();
                //  enemyBoss.init(-1,new Point(1000, 400));
                    Random random = new Random();
                    int rand = random.nextInt(60);
                    enemyBoss.init(-1,new Point(1300, rand * 10),135 + 15*random.nextInt(6));
                    break;
            }
            return false;
        }
    }


}
