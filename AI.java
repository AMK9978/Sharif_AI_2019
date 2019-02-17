package client;

import client.model.*;

import java.util.ArrayList;
import java.util.Random;

public class AI {
    private Random random = new Random();
    private Map map;
    private int pick_period = 0;
    Boolean b = false;

    ArrayList<Cell> Blocked_Cells = new ArrayList<>();

    int Dispersion;
    boolean extraSentry = false;

    public double Variance(ArrayList<Integer> dis)
    {
        double var=0,ave=0,sum2=0;
        int sum1 =0;

        for(int i=0;i<dis.size();i++)
        {
            sum1+=dis.get(i);
        }

        ave = sum1/dis.size();

        for(int i=0;i<dis.size();i++)
        {
            sum2+=Math.pow(dis.get(i)-ave,2);
        }

        var = sum2/dis.size();
        return var;
    }

//    public ArrayList<Cell> getMargine(World world)
//    {
//        Cell[] cells=world.getMap().getObjectiveZone();
//
//        for(int i=0;i<cells.length;i++)
//        {
//
//        }
//    }

    public void preProcess(World world) {
        System.out.println("pre process started");
        map = world.getMap();
        Cell[][] cells = map.getCells();
        ArrayList<Integer> dis =new ArrayList<>();

        for(int i=0;i<31;i++)
        {
            for (int j = 0; j <31 ; j++) {

                if(cells[i][j].isWall())
                    dis.add(world.manhattanDistance(i,j,0,0));
            }
        }

        int delta=0;

        Dispersion = (int) Variance(dis);
        if(Dispersion>delta)
        {
            extraSentry = true;
        }

    }

    public void pickTurn(World world) {

        System.out.println("pick turn!:" + pick_period);
        if (pick_period == 0) {
            world.pickHero(HeroName.SENTRY);
            pick_period++;
        } else if (pick_period == 1) {
            world.pickHero(HeroName.BLASTER);
            pick_period++;
        } else if (pick_period == 2) {
            world.pickHero(HeroName.SENTRY);
            pick_period++;
        } else {
            if (random.nextInt() % 2 == 0) {
                world.pickHero(HeroName.HEALER);
            } else {
                world.pickHero(HeroName.GUARDIAN);
            }

            pick_period = 0;
        }
    }


    public void moveTurn(World world) {
        System.out.println("move started");

        for (int j = 0; j < world.getMyHeroes().length; j++) {
            Blocked_Cells.add(world.getMyHeroes()[j].getCurrentCell());
        }
        Cell[][] cells = world.getMap().getCells();
        Hero My_hero = null;
        System.out.println("Phase in move:" + world.getMovePhaseNum());
        Cell[] targets = world.getMap().getObjectiveZone();
        Cell target = targets[random.nextInt(targets.length)];
        for (int i = 0; i < 4; i++) {
            My_hero = world.getMyHeroes()[i];
            if (My_hero.getCurrentCell().isInObjectiveZone()) {
                continue;
            }
            My_hero = world.getMyHeroes()[i];
            Cell origin = My_hero.getCurrentCell();
            Direction directions[] =
                    world.getPathMoveDirections(origin, target, Blocked_Cells);

            if (world.getCurrentTurn() > 30 && world.getAP() < 75) {
                i = 2;
            } else if (world.getAP() < 75) {
                if (i == 2) {
                    break;
                }
            }
            System.out.println("Length:" + directions.length);
            System.out.println("turn:" + world.getCurrentTurn() + ",id:" + My_hero.getId() + ",row:" + origin.getRow() + ",col:" + origin.getColumn());
            System.out.println("target row:" + target.getRow() + ",col:" + target.getColumn());

            System.out.println(world.getPathMoveDirections(origin, target, Blocked_Cells)[0].toString());
            Direction direction = directions[0];
            world.moveHero(My_hero, direction);
        }
        Blocked_Cells.clear();
    }


    public void actionTurn(World world) {
        System.out.println("action started");
        Hero[] heroes = world.getMyHeroes();
        ArrayList<Cell> Opp_cells = new ArrayList<>();
        Hero[] Opp_Heroes = world.getOppHeroes();

        for (int i = 0; i < 4; i++) {
            if (Opp_Heroes[i].getCurrentCell().isInVision()) {
                Opp_cells.add(Opp_Heroes[i].getCurrentCell());
            }
        }

        for (Hero hero : heroes) {
            Cell hero_cell = hero.getCurrentCell();
            if (hero.getName().equals(HeroName.SENTRY)) {

                if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.isInVision(hero_cell, Opp_cell)) {
                            world.castAbility(hero, AbilityName.SENTRY_RAY, Opp_cell);
                        } else if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()) {
                            if (world.manhattanDistance(hero_cell, Opp_cell) <= hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                world.castAbility(hero, AbilityName.SENTRY_ATTACK, Opp_cell);
                            }
                        }
                    }
                } else {
                    for (Cell Opp_cell : Opp_cells) {
                        if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()) {
                            if (world.manhattanDistance(hero_cell, Opp_cell)
                                    <= hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                world.castAbility(hero, AbilityName.SENTRY_ATTACK, Opp_cell);
                            }
                        }
//                        else if (world.getOppHero(Opp_cells.get(j)).getName().equals(HeroName.SENTRY)
//                                && world.isInVision(world.getOppHero(Opp_cells.get(j)).getCurrentCell(), hero_cell)) {
//                            if ()
//                            world.castAbility(hero, AbilityName.SENTRY_DODGE, );
//                        }
                    }

                }

            } else if (hero.getName().equals(HeroName.BLASTER)) {
                if (hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.BLASTER_BOMB).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.BLASTER_BOMB, Opp_cell);
                        } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady() &&
                                world.manhattanDistance(hero_cell, Opp_cell)
                                        <= hero.getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.BLASTER_ATTACK, Opp_cell);
                        }
                    }
                } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
                    for (Cell opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, opp_cell) <=
                                hero.getAbility(AbilityName.BLASTER_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.BLASTER_ATTACK, opp_cell);
                        }
                    }
                }
            } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                if (hero.getAbility(AbilityName.GUARDIAN_ATTACK).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.GUARDIAN_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, Opp_cell);
                        }
                    }
                }
                if (hero.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) {
                    for (Hero hero1 : world.getMyHeroes()) {
                        Cell des = hero1.getCurrentCell();
                        if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                hero.getAbility(AbilityName.GUARDIAN_FORTIFY).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.GUARDIAN_FORTIFY, des);
                        }
                    }
                }
            } else {
                // Healer
                if (hero.getAbility(AbilityName.HEALER_ATTACK).isReady()) {
                    for (Cell Opp_cell : Opp_cells) {
                        if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                hero.getAbility(AbilityName.HEALER_ATTACK).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, Opp_cell);
                        }
                    }
                }
                if (hero.getAbility(AbilityName.HEALER_HEAL).isReady()) {
                    for (Hero hero1 : world.getMyHeroes()) {
                        Cell des = hero1.getCurrentCell();
                        if (hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                hero.getAbility(AbilityName.HEALER_HEAL).getAreaOfEffect()) {
                            world.castAbility(hero, AbilityName.HEALER_HEAL, des);
                        }
                    }
                }
            }

            if (!hero.getCurrentCell().isInObjectiveZone() && world.getAP() > 0) {
                Cell Objective_cell = world.getMap().getObjectiveZone()[random.nextInt(5)];
                int row = Objective_cell.getRow(), column = Objective_cell.getColumn();
                Direction direction[] = world.getPathMoveDirections(hero_cell, Objective_cell);
                if (direction.length == 0) {
                    continue;
                }
                ArrayList<Cell> targets = new ArrayList<>();
                targets.add(change(direction[0], world, row, column));
                if (hero.getName().equals(HeroName.SENTRY)) {
                    // 3
                    for (int i = 1; i < direction.length && i < 3; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.SENTRY_DODGE, targets.get(targets.size() - 1));
                }
                if (hero.getName().equals(HeroName.BLASTER)) {
                    // 4
                    for (int i = 1; i < direction.length && i < 4; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.BLASTER_DODGE, targets.get(targets.size() - 1));
                }
                if (hero.getName().equals(HeroName.GUARDIAN)) {
                    // 2
                    for (int i = 1; i < direction.length && i < 2; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.GUARDIAN_DODGE, targets.get(targets.size() - 1));
                }
                if (hero.getName().equals(HeroName.HEALER)) {
                    // 4
                    for (int i = 1; i < direction.length && i < 4; i++) {
                        targets.add(change(direction[i], world, targets.get(targets.size() - 1).getRow()
                                , targets.get(targets.size() - 1).getColumn()));
                    }
                    world.castAbility(hero, AbilityName.HEALER_DODGE, targets.get(targets.size() - 1));
                }
            }
        }
    }

    public Cell change(Direction direction, World world, int row, int column) {
        Cell target;
        if (direction.equals(Direction.UP)) {
            target = world.getMap().getCell(row - 1, column);
        } else if (direction.equals(Direction.DOWN)) {
            target = world.getMap().getCell(row + 1, column);
        } else if (direction.equals(Direction.LEFT)) {
            target = world.getMap().getCell(row, column - 1);
        } else {
            target = world.getMap().getCell(row, column + 1);
        }
        return target;
    }

    private Cell is_under_attack(int i, Cell hero_cell, Cell cell, World world) {

        return null;
    }

}
