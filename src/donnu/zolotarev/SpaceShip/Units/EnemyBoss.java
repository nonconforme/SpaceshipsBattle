package donnu.zolotarev.SpaceShip.Units;

import donnu.zolotarev.SpaceShip.AI.EnemyAI.Boss1AI;
import donnu.zolotarev.SpaceShip.AI.TurretAI;
import donnu.zolotarev.SpaceShip.Bullets.BaseBullet;
import donnu.zolotarev.SpaceShip.Bullets.BulletColorRandomizer;
import donnu.zolotarev.SpaceShip.Textures.TextureLoader;
import donnu.zolotarev.SpaceShip.Utils.Constants;
import donnu.zolotarev.SpaceShip.Weapons.Modificator.DamageModificator;
import donnu.zolotarev.SpaceShip.Weapons.Modificator.IWeaponModificator;
import donnu.zolotarev.SpaceShip.Weapons.Modificator.RotateAngleModificator;
import donnu.zolotarev.SpaceShip.Weapons.Modificator.SpeedFireModificator;
import donnu.zolotarev.SpaceShip.Weapons.SimpleGun;
import donnu.zolotarev.SpaceShip.Weapons.WeaponController;
import donnu.zolotarev.SpaceShip.Weapons.WeaponPos;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.ColorModifier;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.util.color.Color;
import org.andengine.util.modifier.IModifier;

import java.util.Random;

public class EnemyBoss extends BaseUnit {

    private TurretAI turret;

    public EnemyBoss(){
        super(0);
        turret = new TurretAI(TextureLoader.getBossTurelTextureRegion(), engine.getVertexBufferObjectManager()){
            @Override
            protected void doAfterUpdate() {
                mX = sprite.getX()+193 - turret.gethW();
                mY = sprite.getY()+143 - turret.gethH();
            }
        };

        sprite = new Boss1AI(TextureLoader.getBossBaseTextureRegion(), engine.getVertexBufferObjectManager()){
            @Override
            protected void doAfterUpdate() {
                weaponController.weaponCooldown();
                checkHitHero();
            }
        };

        turret.setScaleCenter(53,37);
        turret.setRotation(180);
        mainScene.attachChild(turret);
        turret.setZIndex(2);
        attachToScene();
        sprite.setZIndex(-1);
        mainScene.sortChildren();
    }

    @Override
    public boolean addDamageAndCheckDeath(int damage) {
        turret.registerEntityModifier( new ColorModifier(Constants.BLINK_TIME, new Color(Color.WHITE), new Color(1f, 0.5f, 0.5f), new IEntityModifier.IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {

            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                pItem.setColor(new Color(Color.WHITE));

            }
        }));
        return super.addDamageAndCheckDeath(damage);
    }

    @Override
    protected void loadWeapon(int level) {
        weaponController = new WeaponController(new WeaponPos[]{
                new WeaponPos(turret, 125, 23 , 0),
                new WeaponPos(turret, 125, 36 , 0),
                new WeaponPos(turret, 125, 49 , 0),
                new WeaponPos(sprite, 215, 25 , -10),
                new WeaponPos(sprite, 215, 262 , 10),
        });
        weaponController.setShoot(true);
        weaponController.reloadWeapons();
        Random random = new Random();
        IWeaponModificator[] modificator = {new SpeedFireModificator(0.25f,IWeaponModificator.Mode.CHANGE),
                BulletColorRandomizer.getNextColorForEmeny()};
        IWeaponModificator[] moR = {
                new RotateAngleModificator(1f,IWeaponModificator.Mode.PERCENT),
                new SpeedFireModificator(-3f,IWeaponModificator.Mode.PERCENT),
                new DamageModificator(0.6f,IWeaponModificator.Mode.PERCENT)};
        weaponController.loadWeapon(new SimpleGun(false, BaseBullet.TYPE_SIMPLE_BULLET,modificator), 0);
        weaponController.loadWeapon(new SimpleGun(false, BaseBullet.TYPE_SIMPLE_BULLET,modificator), 1);
        weaponController.loadWeapon(new SimpleGun(false, BaseBullet.TYPE_SIMPLE_BULLET,modificator), 2);
        weaponController.loadWeapon(new SimpleGun(false, BaseBullet.TYPE_ROCKET_AUTO,moR), 3);
        weaponController.loadWeapon(new SimpleGun(false, BaseBullet.TYPE_ROCKET_AUTO,moR), 4);
    }

    @Override
    protected void loadParam(int level) {
        defaultSpeed = 230;
        defaultMaxAngle = 0.5f;
        defaultHealth = 75000;
        price = 3000;
    }

    @Override
    public void destroy(Boolean withAnimate) {
        super.destroy(withAnimate);
        turret.setVisible(false);
        turret.setIgnoreUpdate(true);
        turret.restart();
    }

    @Override
    protected void checkHitHero() {
        if (hero != null){
            if (checkHit(hero)){
                    dieListener.heroDie();
                    hero.destroy(true);
            }
        }
    }
}
