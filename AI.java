package client;

import client.model.*;
import client.model.Map;

import java.util.*;

public class AI {
    private Random random = new Random();
    private Map map;
    private int pick_period = 0;
    Boolean b = false;

    ArrayList<Cell> Blocked_Cells = new ArrayList<>();

    int Dispersion;
    boolean extraSentry = false;
    ArrayList<Cell>closestCells=new ArrayList<>();//for keeping closets cells from Respawn zone to objective zone
    HashMap<Hero,Integer>myHeroesHp = new HashMap<>();
    PriorityQueue<NiceCell> niceCells = new PriorityQueue<>();

    //adding this class to have enough information in order to move
    class NiceCell
    {
        public double score =0;
        public Hero hero;
        public Direction direction = Direction.DOWN;
        public int  targetCellRow = 0;
        public int targetCellColl = 0;
        public String heroName = "";
    }


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

    public ArrayList<Cell> getClosestCells(World world)
    {
        Map map=world.getMap();
        Cell[] MyRespawnZone = world.getMap().getMyRespawnZone();
        Cell[] cells=world.getMap().getObjectiveZone();
        ArrayList<Cell>closestCells = new ArrayList<>();


        Cell minCell = null;
        for(int i=0;i<MyRespawnZone.length;i++)
        {
            int min = 9999;
            for (int j = 0; j <cells.length ; j++) {
               int num= world.manhattanDistance(MyRespawnZone[i],cells[j]);
               if(num<min)
               {
                   min = num;
                   minCell = cells[j];
               }
            }
            closestCells.add(minCell);
        }

        return closestCells;
    }

    //checking whether the intended cell is in objective zone or not
    boolean IsObjectiveZone(int cellRow,int cellCol,World world)
    {
        Cell[] objectiveZone = world.getMap().getObjectiveZone();

        for (int i = 0; i <objectiveZone.length ; i++) {
            if(cellRow == objectiveZone[i].getRow() && cellCol == objectiveZone[i].getColumn())
                return true;
        }
        return false;
    }

    //checking whether the intended cell is wall or not
    boolean IsWall(int cellRow,int cellCol,World world)
    {
        return world.getMap().getCell(cellRow,cellCol).isWall();
    }

    //finding best cell to move
    public void finding_good_cell_to_move(Hero[] My_heroes,World world)
    {
        boolean isSentry = false,isBlaster = false,isHealer = false,isGuardian = false;
        int cnt = 0;
        for(Hero hero:My_heroes)
        {
            if(hero.getName().equals(HeroName.HEALER)) {
                isHealer = true;
                break;
            }
            cnt++;
        }

        Hero Healer = My_heroes[cnt];
        if(!Healer.getName().equals(HeroName.HEALER))
            Healer = null;

        Hero intended_hero = My_heroes[cnt];
        Hero[] oppHeroes = world.getOppHeroes();
        double max = -9999;
        Direction direction = Direction.UP;
        String heroName = "";
        double score = 0;

        Cell cell = world.getMap().getCell(0,0);

        int niceTargetRow = 0;
        int niceTargetColl = 0;

        for(Hero hero:My_heroes) {
            max = -9999;

            if (hero.getName().equals(HeroName.SENTRY)) {
                isSentry = true;
                isBlaster = false;
                isHealer = false;
                isGuardian = false;
            } else if (hero.getName().equals(HeroName.BLASTER)) {
                isBlaster = true;
                isSentry = false;
                isHealer = false;
                isGuardian = false;
            } else if (hero.getName().equals(HeroName.HEALER)) {
                isHealer = true;
                isSentry = false;
                isBlaster = false;
                isGuardian = false;
            } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                isGuardian = true;
                isSentry = false;
                isBlaster = false;
                isHealer = false;
            }

            cell = hero.getCurrentCell();

            //moving up
            {
                if (cell.getRow() - 1 >= 0) {

                    Cell targetCell = world.getMap().getCell(cell.getRow()-1,cell.getColumn());

                    if (targetCell.isInObjectiveZone())
                        score += 1;

                    if (targetCell.isWall())
                        score -= 1;

                    if (Healer != null) {

                        Cell healerCell = Healer.getCurrentCell();
                        if (!isHealer && world.manhattanDistance(cell,healerCell) <= 4)
                            score += .5;
                    }

                    if (hero.getCurrentHP() - myHeroesHp.get(hero) < 0)
                        score -= 2;

                    for (Hero oppHero : oppHeroes) {
                        Cell oppCell = oppHero.getCurrentCell();

                        if (oppHero.getName().equals(HeroName.SENTRY)) {
                            score -= 1;
                        } else if (oppHero.getName().equals(HeroName.BLASTER)) {

                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;

                        } else if (oppHero.getName().equals(HeroName.GUARDIAN)) {

                            if (world.manhattanDistance(targetCell, oppCell) > 1)
                                score += 1;
                            else
                                score -= 1;

                        } else if (oppHero.getName().equals(HeroName.HEALER)) {

                            if (world.manhattanDistance(targetCell,oppCell)> 4)
                                score += 1;
                            else
                                score -= 1;
                        }

                    }

                    if (max < score) {
                        max = score;
                        niceTargetRow = cell.getRow() - 1;
                        niceTargetColl = cell.getColumn();
                        intended_hero = world.getMyHero(cell);
                        direction = Direction.UP;
                    }
                }
            }

            //moving down
            {
                if (cell.getRow() + 1 < 31 && !cell.isWall()) {

                    Cell targetCell = world.getMap().getCell(cell.getRow()+1,cell.getColumn());

                    if (targetCell.isInObjectiveZone())
                        score += 1;

                    if (targetCell.isWall())
                        score -= 1;

                    if (Healer != null) {
                        Cell healerCell = Healer.getCurrentCell();

                        if (!isHealer && world.manhattanDistance(cell,healerCell) <= 4)
                            score += .5;
                    }

                    if (hero.getCurrentHP() - myHeroesHp.get(hero) < 0)
                        score -= 2;

                    for (Hero oppHero : oppHeroes) {
                        Cell oppCell = oppHero.getCurrentCell();
                        if (oppHero.getName().equals(HeroName.SENTRY)) {
                            score -= 1;
                        } else if (oppHero.getName().equals(HeroName.BLASTER)) {

                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;
                        } else if (oppHero.getName().equals(HeroName.GUARDIAN)) {
                            if (world.manhattanDistance(targetCell, oppCell) > 1)
                                score += 1;
                            else
                                score -= 1;
                        } else if (oppHero.getName().equals(HeroName.HEALER)) {
                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;
                        }

                    }

                    if (max < score) {
                        max = score;
                        niceTargetRow = cell.getRow() + 1;
                        niceTargetColl = cell.getColumn();
                        direction = Direction.DOWN;
                        intended_hero = world.getMyHero(cell);
                    }
                }
            }
            //moving left
            {
                if (cell.getColumn() - 1 >= 0 && !cell.isWall()) {

                    Cell targetCell = world.getMap().getCell(cell.getRow(),cell.getColumn() - 1);

                    if (targetCell.isInObjectiveZone())
                        score += 1;

                    if (targetCell.isWall())
                        score -= 1;

                    if (Healer != null) {
                        Cell healerCell = Healer.getCurrentCell();

                        if (!isHealer && world.manhattanDistance(cell,healerCell) <= 4)
                            score += .5;
                    }

                    if (hero.getCurrentHP() - myHeroesHp.get(hero) < 0)
                        score -= 2;

                    for (Hero oppHero : oppHeroes) {

                        Cell oppCell = oppHero.getCurrentCell();
                        if (oppHero.getName().equals(HeroName.SENTRY)) {
                            score -= 1;
                        } else if (oppHero.getName().equals(HeroName.BLASTER)) {
                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;
                        } else if (oppHero.getName().equals(HeroName.GUARDIAN)) {
                            if (world.manhattanDistance(targetCell, oppCell) > 1)
                                score += 1;
                            else
                                score -= 1;
                        } else if (oppHero.getName().equals(HeroName.HEALER)) {
                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;
                        }

                    }

                    if (max < score) {
                        max = score;
                        niceTargetRow = cell.getRow();
                        niceTargetColl = cell.getColumn() - 1;
                        direction = Direction.LEFT;
                        intended_hero = world.getMyHero(cell);
                    }
                }
            }
            //moving right
            {
                if (cell.getColumn() + 1 < 31 && !cell.isWall()) {

                    Cell targetCell = world.getMap().getCell(cell.getRow(),cell.getColumn()+1);

                    if (targetCell.isInObjectiveZone())
                        score += 1;

                    if (targetCell.isWall())
                        score -= 1;

                    if (Healer != null) {

                        Cell healerCell = Healer.getCurrentCell();

                        if (!isHealer && world.manhattanDistance(cell, healerCell) <= 4)
                            score += .5;
                    }

                    if (hero.getCurrentHP() - myHeroesHp.get(hero) < 0)
                        score -= 2;

                    for (Hero oppHero : oppHeroes) {

                        Cell oppCell = oppHero.getCurrentCell();

                        if (oppHero.getName().equals(HeroName.SENTRY)) {
                            score -= 1;
                        } else if (oppHero.getName().equals(HeroName.BLASTER)) {

                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;

                        } else if (oppHero.getName().equals(HeroName.GUARDIAN)) {

                            if (world.manhattanDistance(targetCell,oppCell) > 1)
                                score += 1;
                            else
                                score -= 1;

                        } else if (oppHero.getName().equals(HeroName.HEALER)) {
                            if (world.manhattanDistance(targetCell, oppCell) > 4)
                                score += 1;
                            else
                                score -= 1;
                        }

                    }

                    if (max < score) {
                        max = score;
                        niceTargetRow = cell.getRow();
                        niceTargetColl = cell.getColumn() + 1;
                        direction = Direction.RIGHT;
                        intended_hero = world.getMyHero(cell);

                    }
                }
            }
        }

        if (isSentry) {
            heroName = "sentry";
        } else if (isBlaster) {
            heroName = "blaster";
        } else if (isHealer) {
            heroName = "healer";
        } else if (isGuardian) {
            heroName = "guardian";
        }

        NiceCell nice_cell = new NiceCell();
        nice_cell.score = max;
        nice_cell.hero = intended_hero;
        nice_cell.targetCellRow = niceTargetRow ;
        nice_cell.targetCellColl = niceTargetColl;
        nice_cell.heroName = heroName;
        nice_cell.direction = direction;

        niceCells.add(nice_cell);
    }

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

        closestCells = getClosestCells(world);

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

        Hero[] heroes = world.getMyHeroes();
        Cell[][] cells = world.getMap().getCells();
        Hero My_hero;
        Hero intended_hero;

        System.out.println("Phase in move:" + world.getMovePhaseNum());
        Cell[] targets = world.getMap().getObjectiveZone();
        Cell target = targets[random.nextInt(targets.length)];

        for (int i = 0; i <4 ; i++) {
            My_hero = world.getMyHeroes()[i];

            if (My_hero.getCurrentCell().isInObjectiveZone()) {
                NiceCell niceCell = niceCells.peek();
                world.moveHero(niceCell.hero,niceCell.direction);
                niceCells.clear();
            }

            myHeroesHp.put(My_hero,My_hero.getCurrentHP());
            Cell origin = My_hero.getCurrentCell();
            Direction directions[] =
                    world.getPathMoveDirections(origin, target, Blocked_Cells);

            Direction direction = directions[0];
            world.moveHero(My_hero, direction);

        }

//        for (int i = 0; i < 4; i++) {
//            My_hero = world.getMyHeroes()[i];
//            if (My_hero.getCurrentCell().isInObjectiveZone() && My_hero.getCurrentHP()== myHeroesHp.get(My_hero)) {
//                continue;
//            }else if (My_hero.getCurrentCell().isInObjectiveZone() && My_hero.getCurrentHP()<myHeroesHp.get(My_hero))
//            {
//
//            }
//            else if (!My_hero.getCurrentCell().isInObjectiveZone()){
//
//            }
//            myHeroesHp.put(My_hero,My_hero.getCurrentHP());
//
//            My_hero = world.getMyHeroes()[i];
//            Cell origin = My_hero.getCurrentCell();
//            Direction directions[] =
//                    world.getPathMoveDirections(origin, target, Blocked_Cells);
//
//            if (world.getCurrentTurn() > 30 && world.getAP() < 75) {
//                i = 2;
//            } else if (world.getAP() < 75) {
//                if (i == 2) {
//                    break;
//                }
//            }
//            System.out.println("Length:" + directions.length);
//            System.out.println("turn:" + world.getCurrentTurn() + ",id:" + My_hero.getId() + ",row:" + origin.getRow() + ",col:" + origin.getColumn());
//            System.out.println("target row:" + target.getRow() + ",col:" + target.getColumn());
//
//            System.out.println(world.getPathMoveDirections(origin, target, Blocked_Cells)[0].toString());
//            Direction direction = directions[0];
//            world.moveHero(My_hero, direction);
//
//        }
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
