package donnu.zolotarev.SpaceShip.Scenes;

import android.graphics.Point;
import donnu.zolotarev.SpaceShip.Bullets.BaseBullet;
import donnu.zolotarev.SpaceShip.Effects.Fire;
import donnu.zolotarev.SpaceShip.GameData.UserDataProcessor;
import donnu.zolotarev.SpaceShip.GameState.IAddListener;
import donnu.zolotarev.SpaceShip.GameState.IAmDie;
import donnu.zolotarev.SpaceShip.GameState.IParentScene;
import donnu.zolotarev.SpaceShip.GameState.IStatusGameInfo;
import donnu.zolotarev.SpaceShip.Levels.LevelInfo;
import donnu.zolotarev.SpaceShip.R;
import donnu.zolotarev.SpaceShip.Scenes.Interfaces.ISimpleClick;
import donnu.zolotarev.SpaceShip.Textures.TextureLoader;
import donnu.zolotarev.SpaceShip.Units.BaseUnit;
import donnu.zolotarev.SpaceShip.Units.Hero;
import donnu.zolotarev.SpaceShip.Utils.Constants;
import donnu.zolotarev.SpaceShip.Utils.MenuFactory;
import donnu.zolotarev.SpaceShip.Utils.Utils;
import donnu.zolotarev.SpaceShip.Waves.IWaveController;
import org.andengine.entity.particle.emitter.PointParticleEmitter;

import java.util.Random;

public class MaketGameScene extends BaseGameScene implements IAmDie {

    private final Random random;
    private LevelInfo levelInfo;
    private boolean isAlreadyProcess = false;
    private int lastRand = 0;


    private Point point;

    public MaketGameScene(IParentScene selectionLevelScene, LevelInfo level) {
       super(selectionLevelScene);
       this.levelInfo = level;
        random = new Random();
        point = new Point();
     //   qwet();
    }

    private void qwet() {
        PointParticleEmitter emitter=  new PointParticleEmitter(0,0);
        Fire fire =  new Fire(emitter);
        emitter.setCenter(500,200);

        attachChild(fire);
    }

    @Override
    public void addNewWaveController(IWaveController controller) {
        ((IAddListener)controller).addListener(new IStatusGameInfo() {
            @Override
            public void onNextWave(int count) {
               /* waveIndex++;
                textWaveBarCallback.onNextWave(waveIndex);*/
            }

            @Override
            public void onWinLevel() {
                beforeReturnToParent(IParentScene.EXIT_WIN);
                final String str = shipActivity.getString(R.string.text_win_message,score);
                shipActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isActive = false;
                        enablePauseMenu = false;
                        setChildScene(MenuFactory.createMenu(engine, shipActivity.getCamera(), shipActivity)
                                .addedText(str, TextureLoader.getFont())
                                .addedItem(TextureLoader.getMenuButtonBackgroundTextureRegion(), R.string.btn_game_back_to_main_menu,
                                        TextureLoader.getFont(), new ISimpleClick() {
                                            @Override
                                            public void onClick(int id) {

                                                returnToParentScene(IParentScene.EXIT_WIN);
                                            }
                                        })
                                .enableAnimation()
                                .build(), false, true, true);
                    }
                });
            }
        });
        super.addNewWaveController(controller);
    }

    @Override
    protected void initWave() {
    }

    @Override
    protected void initHero() {
        hero = new Hero(textHealthBarCallback, setting.isAnalogflyControlMode());
        hero.init(0, new Point(600, 300));
    }

    @Override
    protected void initBulletsPools() {
        BaseBullet.initPool();
        BaseBullet.setUnitDieListener(this);
        BaseUnit.setUnitDieListener(this);
    }

    @Override
    protected void initUnitsPools() {
        BaseUnit.initPool();
    }

    @Override
    public boolean addEnemy(AddedEnemyParam param) {
        if (super.addEnemy(param)){
            BaseUnit enemy1 = BaseUnit.getEnemy(Constants.MAX_UNIT_LEVEL_WITH_SHIELD * (param.getKind() / Constants.MAX_UNIT_LEVEL_WITH_SHIELD));


           // PointF pointF = activeScene.getHero().getPosition();
            if (param.getStartPosition() == null){
                int rand = random.nextInt(60);
                if (Utils.equals(lastRand, rand, 10)){
                    rand = 35 + (int) Utils.random(- 10, 15);
                }
                lastRand = rand;
                point.set(1300, lastRand * 10);
            }else {
                point = param.getStartPosition();
            }
            // todo
            enemy1.init(param.getKind() % Constants.MAX_UNIT_LEVEL_WITH_SHIELD, point, param.getStartAngle()/*Utils.getAngle(point.x, point.y, pointF.x, pointF.y)*/);
        }
        return true;
    }

    @Override
    protected void beforeReturnToParent(int status) {
        UserDataProcessor dataProcessor = UserDataProcessor.get();
        if (status == IParentScene.EXIT_WIN || status == IParentScene.EXIT_DIE){
            boolean flag = status == IParentScene.EXIT_WIN;
            if (levelInfo != null){
                flag  = flag && !levelInfo.isWin();
            }
            if( !isAlreadyProcess){
                score =  dataProcessor.processGold(score,flag);
                levelInfo.addTotalCoast(score);
            }
            isAlreadyProcess = true;

        }
        levelInfo = null;
    }

    @Override
    public void destroyed(BaseUnit o) {
        waveIndex++;
        textWaveBarCallback.onNextWave(waveIndex);
        addToScore(o.getMoney() + (int) Utils.random(0,5));
    }
}
