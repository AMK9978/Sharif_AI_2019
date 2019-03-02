package client;

import client.model.*;
import client.model.Map;

import java.util.*;

public class AI {
    private Random random = new Random();
    private int pick_period = 0;
    Boolean b = false;
    private ArrayList<Cell> blocks = new ArrayList<>();
    private ArrayList<Cell> Blocked_Cells = new ArrayList<>();
    private boolean extraSentry = false;
    // to objective zone
    private HashMap<Hero, Integer> myHeroesHp = new HashMap<>();
    private PriorityQueue<NiceCell> niceCells = new PriorityQueue<>();

    //adding this class to have enough information in order to move
    class NiceCell implements Comparable<NiceCell> {
        double score = 0;
        Hero hero;
        Direction direction = Direction.DOWN;
        Cell target;
        String heroName = "";

        @Override
        public int compareTo(NiceCell niceCell) {
            if (this.score < niceCell.score) {
                return 1;
            } else {
                return -1;
            }
        }
    }


    private double Variance(ArrayList<Integer> dis) {
        double var = 0, ave = 0, sum2 = 0;
        int sum1 = 0;

        for (Integer di : dis) {
            sum1 += di;
        }

        ave = sum1 / dis.size();

        for (Integer di : dis) {
            sum2 += Math.pow(di - ave, 2);
        }

        var = sum2 / dis.size();
        return var;
    }

    private Cell getClosestCells(World world, Hero hero) {
        Cell[] cells = world.getMap().getObjectiveZone();
        //A rrayList<Cell> closestCells = new ArrayList<>();
//        ArrayList<Cell>cellArrayList = new ArrayList<>();
        Cell minCell = null;
        int min = 9999;
        for (Cell cell : cells) {
            int i;
            if(cell.isWall())
                continue;
            Boolean hast=false;
            for(i = 0; i < world.getMyHeroes().length; i++) {
                if (world.getMyHeroes()[i].getCurrentCell().equals(cell)) {
                    hast = true;
                    break;
                }
            }
            if(hast)
                continue;
            if (i == world.getMyHeroes().length) {
                int num = world.manhattanDistance(hero.getCurrentCell(), cell);
//                cellArrayList.add(cell);
                if (num < min) {
                    min = num;
                    minCell = cell;
                }
            }
        }
     
//        if (closestCells.size()!=0){
        return minCell;
//        }else{
//            return cellArrayList;
//        }
    }

    //finding the best cell to move
    private void finding_good_cell_to_move(Hero[] My_heroes, Hero hero, World world) {
        boolean isSentry = false, isBlaster = false, isHealer = false, isGuardian = false;
        
        Hero Healer = null;
        for (Hero hero2 : My_heroes) {
            if (hero2.getName().equals(HeroName.HEALER)) {
                Healer = hero2;
                break;
            }
        }
        Hero intended_hero = null;
        double max = -9999;
        Direction direction = Direction.UP;
        String heroName = "";
        double score = 0;
        Cell cell = hero.getCurrentCell();
        int niceTargetRow = 0;
        int niceTargetColl = 0;
        switch (hero.getName()) {
            case SENTRY:
                isSentry = true;
                isBlaster = false;
                isHealer = false;
                isGuardian = false;
                break;
            case BLASTER:
                isBlaster = true;
                isSentry = false;
                isHealer = false;
                isGuardian = false;
                break;
            case HEALER:
                isHealer = true;
                isSentry = false;
                isBlaster = false;
                isGuardian = false;
                break;
            case GUARDIAN:
                isGuardian = true;
                isSentry = false;
                isBlaster = false;
                isHealer = false;
                break;
            default:
                break;
        }

        //dont move
            Cell targetCell = cell;
            score = getCellScore(hero, world, targetCell, null, Healer, isHealer);
            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow();
                niceTargetColl = cell.getColumn();
                direction = null;
            }

        //moving up
        if (world.getMap().isInMap(cell.getRow() - 1, cell.getColumn()) &&
                (world.getMap().getCell(cell.getRow() - 1, cell.getColumn()).isWall()==false) ) {

            targetCell = world.getMap().getCell(cell.getRow() - 1, cell.getColumn());

            score = getCellScore(hero, world, targetCell, Direction.UP, Healer, isHealer);
            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow() - 1;
                niceTargetColl = cell.getColumn();
                direction = Direction.UP;
            }
        }

        //moving down
        if (world.getMap().isInMap(cell.getRow() + 1, cell.getColumn()) &&
                !world.getMap().getCell(cell.getRow() + 1, cell.getColumn()).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow() + 1, cell.getColumn());
            score = getCellScore(hero, world, targetCell, Direction.DOWN, Healer, isHealer);

            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow() + 1;
                niceTargetColl = cell.getColumn();
                direction = Direction.DOWN;
            }
        }

        //moving left
        if (world.getMap().isInMap(cell.getRow(), cell.getColumn() - 1) &&
                !world.getMap().getCell(cell.getRow(), cell.getColumn() - 1).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() - 1);
            score = getCellScore(hero, world, targetCell, Direction.LEFT, Healer, isHealer);

            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow();
                niceTargetColl = cell.getColumn() - 1;
                direction = Direction.LEFT;
            }
        }

        //moving right
        if (world.getMap().isInMap(cell.getRow(), cell.getColumn() + 1) &&
                !world.getMap().getCell(cell.getRow(), cell.getColumn() + 1).isWall()) {

            targetCell = world.getMap().getCell(cell.getRow(), cell.getColumn() + 1);
            score = getCellScore(hero, world, targetCell, Direction.RIGHT, Healer, isHealer);

            if (max < score) {
                max = score;
                niceTargetRow = cell.getRow();
                niceTargetColl = cell.getColumn() + 1;
                direction = Direction.RIGHT;
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
        //System.out.println("hero:" + heroName + ",score:" + score);
        NiceCell nice_cell = new NiceCell();
        nice_cell.score = max;
        nice_cell.hero = hero;
        nice_cell.target = world.getMap().getCell(niceTargetRow, niceTargetColl);
        nice_cell.heroName = heroName;
        nice_cell.direction = direction;
        niceCells.add(nice_cell);
    }
    private ArrayList<Cell> getClosestCellsForDodge(World world, Hero hero,int dodge_board) {
        Cell[] cells = world.getMap().getObjectiveZone();
        ArrayList<Cell> closestCells = new ArrayList<>();
        Cell minCell = null;
        for (Cell cell : cells) {
            if (!isOccupied(cell, world) && !Blocked_Cells.contains(cell) && !blocks.contains(cell)
                    && world.manhattanDistance(hero.getCurrentCell(),cell) <= dodge_board ) {
                minCell = cell;
            }
        }
        if (minCell != null) {
            closestCells.add(minCell);
        }else{
            Random random = new Random();
            closestCells.add(world.getMap().getObjectiveZone()[Math.abs(random.nextInt()%20)]);
        }
        return closestCells;
    }
    private double getCellScoreForAction(World world,Hero hero, Ability ability,Hero[] oppHeroes
            ,HashMap<Hero,Integer> healths,ArrayList<Hero> list, double maxScore)
    {
        int score = 0;
        ArrayList<Hero> lis = new ArrayList<>();
        for(Hero oHero : oppHeroes)
        {
            if(oHero.getCurrentCell().isInVision() ||oHero.getCurrentHP() <= 0)
                continue;
            if(world.manhattanDistance(hero.getCurrentCell(), hero.getCurrentCell()) 
                    <= ability.getRange() + ability.getAreaOfEffect())
            {
                score += 500- healths.get(oHero);
                lis.add(oHero);
            }
        }
        if(score > maxScore)
        {
            list.clear();
            for(Hero aHero : lis)
            {
                list.add(aHero);
            }
        }
        return score;
    }
    private double getCellScore(Hero hero, World world, Cell targetCell, Direction direction, Hero Healer, Boolean isHealer) 
    {
        if (targetCell.isWall()) {
            return -100;
        }
        if (direction != null && Blocked_Cells.contains(targetCell)) {
            return -100;
        }
        double score = 0;
        Cell cell = hero.getCurrentCell();
        Hero[] oppHeroes = world.getOppHeroes();
        HeroName name = hero.getName();
        Hero[] myHeroes = world.getMyHeroes();
        if (targetCell.isInObjectiveZone())
        {
            score += 2.5;
        }
        else
        {
            Cell goodCell = getClosestCells(world, hero);
            if ((world.getPathMoveDirections(cell, goodCell,Blocked_Cells).length == 0 && direction == null)
                    || (world.getPathMoveDirections(cell, goodCell,Blocked_Cells).length > 0 
                    && world.getPathMoveDirections(cell, goodCell,Blocked_Cells)[0] == direction))
                score += 2.5;
        }
        int powerRange=0,attackRange=0,dodgeRange;
        Boolean powerReady=false,attackReady=false;
        Ability powerAbility=null,attackAbility=null;
        if(name.equals(HeroName.BLASTER))
        {
            powerAbility = hero.getAbility(AbilityName.BLASTER_BOMB);
            powerReady = powerAbility.isReady();
            if(powerReady)
                powerRange = powerAbility.getRange();
            
            attackAbility = hero.getAbility(AbilityName.BLASTER_ATTACK);
            attackReady = attackAbility.isReady();
            if(attackReady)
                attackRange = attackAbility.getRange();
            dodgeRange = hero.getAbility(AbilityName.BLASTER_DODGE).getRange();
        }
        else if(name.equals(HeroName.SENTRY))
        {
            powerAbility = hero.getAbility(AbilityName.SENTRY_RAY);
            powerReady = powerAbility.isReady();
            if(powerReady)
                powerRange = powerAbility.getRange();
            
            attackAbility = hero.getAbility(AbilityName.SENTRY_ATTACK);
            attackReady = attackAbility.isReady();
            if(attackReady)
                attackRange = attackAbility.getRange();
            dodgeRange = hero.getAbility(AbilityName.SENTRY_DODGE).getRange();
        }
        else if(name.equals( HeroName.HEALER))
        {
            powerAbility = hero.getAbility(AbilityName.HEALER_HEAL);
            powerReady = powerAbility.isReady();
            if(powerReady)
                powerRange = powerAbility.getRange();
            
            attackAbility = hero.getAbility(AbilityName.HEALER_ATTACK);
            attackReady = attackAbility.isReady();
            if(attackReady)
                attackRange = attackAbility.getRange();
            
            dodgeRange = hero.getAbility(AbilityName.HEALER_DODGE).getRange();
        }
        else
        {
            powerAbility = hero.getAbility(AbilityName.GUARDIAN_FORTIFY);
            powerReady = powerAbility.isReady();
            if(powerReady)
                powerRange = powerAbility.getRange();
            
            attackAbility = hero.getAbility(AbilityName.GUARDIAN_ATTACK);
            attackReady = attackAbility.isReady();
            if(attackReady)
                attackRange = attackAbility.getRange();
            
            dodgeRange = hero.getAbility(AbilityName.GUARDIAN_DODGE).getRange();
        }
        
        Cell[] objective = world.getMap().getObjectiveZone();
        int enteha = 0,ebteda=0;
        for (Cell aCell : objective) {
            if(aCell.getColumn()> enteha)
            {
                if(enteha == 0)
                {
                    ebteda = aCell.getColumn();
                }
                enteha = aCell.getColumn();
            }
            else
            {
                break;
            }
        }

        if ((name.equals(HeroName.BLASTER) && powerReady) || name.equals(HeroName.HEALER))
        {
            if(powerReady || attackReady)
            {
                Cell goodCell = null;
                int attack = 0, power = 0;
                for(int i=ebteda-2; i<=enteha+2; i++)
                {
                    for(int j=ebteda-2; j<=enteha+2; j++)
                    {
                        Cell aCell = world.getMap().getCell(i, j);
                        
                        
                        int enemyScore = 0;
                        for (Hero oppHeroe : oppHeroes) {
                            if(!oppHeroe.getCurrentCell().isInVision()|| oppHeroe.getCurrentHP() <=0)
                                continue;
                            Cell hCell = oppHeroe.getCurrentCell();
                            power =0;
                            attack = 0;
                            if(!world.isInVision(hCell, aCell))
                            {
                                if(powerReady && 
                                        world.manhattanDistance(hCell, aCell) < powerRange 
                                        + powerAbility.getAreaOfEffect())  
                                    power++;
                                else if(attackReady && world.manhattanDistance(hCell, aCell)< attackRange)
                                    attack++;
                                    
                            }
                            else
                            {
                              
                              //some code
                            }
         
                        }
                        if(power+attack > 0)
                        {
                            if( (power*2)+ (attack *2) > enemyScore)
                            {
                                enemyScore =(power*2)+ (attack *2);
                                goodCell = aCell;
                            }
                        }
                    }
                   
                }
                 if(world.getPathMoveDirections(cell, goodCell, Blocked_Cells).length >0)
                    if(world.getPathMoveDirections(cell, goodCell, Blocked_Cells)[0] == direction)
                    {
                        score += (power * 0.9) + (attack * 0.7);
                    }
                    else if(direction == null)
                        score += (power  * 0.9) + (attack * 0.7);
            }
            else
            {
                //now should scape
                for(Hero uHero : oppHeroes)
                {
                    if(!uHero.getCurrentCell().isInVision()|| uHero.getCurrentHP() <=0)
                                continue;
                    for(Ability ab : uHero.getAbilities())
                    {
                        if(world.manhattanDistance(targetCell, uHero.getCurrentCell()) < 
                            ab.getRange() + ab.getAreaOfEffect())
                            score -=0.5;
                    }
                    
                }
            }
            
            if(name.equals(HeroName.BLASTER))
            {
                for (Hero oHero : oppHeroes) 
                {
                    if(oHero.getName() == HeroName.BLASTER && 
                            !oHero.getCurrentCell().isInVision()|| oHero.getCurrentHP() <=0)
                                continue;
                    Cell heroCell = oHero.getCurrentCell();
                    
                    if (powerReady && world.manhattanDistance(heroCell, targetCell)
                           <= powerRange + powerAbility.getAreaOfEffect())
                    {
                        score += 0.8;
                    }


                    if (world.isInVision(targetCell, heroCell) && attackReady
                           && world.manhattanDistance(heroCell, targetCell)
                            <= attackRange +attackAbility.getAreaOfEffect())
                        score += 0.6;
                }
            }      
        } 
        else if (name.equals(HeroName.SENTRY)) {
            if(powerReady || attackReady)
            {
                for (Hero oHero : oppHeroes) 
                {
                    if(oHero.getName() == HeroName.SENTRY && 
                            !oHero.getCurrentCell().isInVision()|| oHero.getCurrentHP() <=0)
                                continue;
                    Cell heroCell = oHero.getCurrentCell();
                    if (world.isInVision(heroCell, targetCell))
                    {
                        if (powerReady)
                            score += 2;
                        if (attackReady
                                && world.manhattanDistance(heroCell, targetCell) <= attackRange )
                            score += 1;

                    }

                }
            }
            else
            {
                for(Hero uHero : oppHeroes)
                {
                    if(!uHero.getCurrentCell().isInVision()|| uHero.getCurrentHP() <=0)
                                continue;
                    for(Ability ab : uHero.getAbilities())
                    {
                        if(world.manhattanDistance(targetCell, uHero.getCurrentCell()) < 
                            ab.getRange() + ab.getAreaOfEffect())
                            score -=0.5;
                    }
                    
                }
            }

        }
        else if(name.equals(HeroName.GUARDIAN))
        {
            int minHP = 1000;
            Hero tHero = null;
            if(attackReady)
            {
                Boolean motamarkez = false;
                for(Hero dHero : oppHeroes)
                {
                    if(!dHero.getCurrentCell().isInVision()|| dHero.getCurrentHP() <=0)
                                continue;
                    if(dHero.getCurrentHP() <minHP)
                    {
                        minHP = dHero.getCurrentHP();
                        tHero = dHero;
                        motamarkez = true;
                    }
                }
                if(motamarkez)
                {
                    if(world.getPathMoveDirections(cell, tHero.getCurrentCell()).length >0)
                    {
                        if(direction == world.getPathMoveDirections(cell, tHero.getCurrentCell())[0])
                            score += 3;
                    }
                    else if(direction == null)
                    {
                            score += 3;
                    }
                    
                }
                else
                {
                    for(Hero dHero : oppHeroes)
                    {
                        if(dHero.getCurrentCell().isInVision() && dHero.getCurrentHP() > 0 )
                        {
                            if(dHero.getCurrentHP() < minHP)
                            {
                                minHP = dHero.getCurrentHP();
                                tHero = dHero;
                            }

                        }
                    }
                    if(tHero != null)
                    {
                        if(world.getPathMoveDirections(cell, tHero.getCurrentCell()).length >0)
                        {
                            if(direction == world.getPathMoveDirections(cell, tHero.getCurrentCell())[0])
                                score += 3;
                        }
                        else if(direction == null)
                        {
                                   score += 3;
                        }
                    }        
                }
                
            }
            else if(powerReady)
            {
                minHP = 1000;
                Hero komak = null;
                for(Hero myHero : myHeroes)
                {
                    if(myHero.getCurrentHP() <= 0)
                        continue;
                    if(myHero.getCurrentHP() < minHP && 
                            world.manhattanDistance(cell, myHero.getCurrentCell()) < 6)
                        komak = myHero;
                
                }
                if(komak != null)
                {
                    if(world.getPathMoveDirections(cell, komak.getCurrentCell() ).length > 0)
                    {
                        if(world.getPathMoveDirections(cell, komak.getCurrentCell() )[0] == direction)
                            score += 3;
                    }
                    else if(direction ==null)
                        score += 3;
                }
            }
        }
        if (!isHealer && Healer != null) {

            Cell healerCell = Healer.getCurrentCell();
            if (world.manhattanDistance(cell, targetCell) ==5 && world.manhattanDistance(targetCell, healerCell) <= 4)
                score += 0.5;
        }
        
        
        if(direction != null)
            score -= 5/hero.getMoveAPCost();
//        
        int counter = 0;
        int ave_row = 0, ave_col = 0;
        int sum_rows = 0, sum_cols = 0;
        for (int i = 0; i < world.getMyHeroes().length; i++) {
            if (world.getMyHeroes()[i].getCurrentHP() > 0) {
                sum_cols += world.getMyHeroes()[i].getCurrentCell().getColumn();
                sum_rows += world.getMyHeroes()[i].getCurrentCell().getRow();
                counter++;
            }
        }
        if (counter != 0) {
            ave_row = Math.round(sum_rows / counter);
            ave_col = Math.round(sum_cols / counter);
            if(world.manhattanDistance(ave_row,ave_col , targetCell.getRow(),targetCell.getColumn()) <2)
                score -= (2-world.manhattanDistance(world.getMap().getCell(ave_row, ave_col), targetCell)) * (0.3);
        }
        return score;
    }
    
    public void preProcess(World world) {
        System.out.println("pre process started");
        //for(Cell bCell : world.getMap().getMyRespawnZone())
            //System.out.println(bCell.getRow()+" "+bCell.getColumn());
        Map map = world.getMap();
        Cell[][] cells = map.getCells();
        ArrayList<Integer> dis = new ArrayList<>();

        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 31; j++) {
                if (cells[i][j].isWall())
                    dis.add(world.manhattanDistance(i, j, 0, 0));
            }
        }

        int delta = 250;

        int dispersion = (int) Variance(dis);
        System.out.println("dispersion:" + dispersion);
        if (dispersion > delta) {
            extraSentry = true;
        }
//        closestCells = getClosestCells(world);

    }

    public void pickTurn(World world) {

        System.out.println("pick turn!:" + pick_period);
        if (pick_period == 0) {
            world.pickHero(HeroName.BLASTER);
            pick_period++;
        } else if (pick_period == 1) {
            world.pickHero(HeroName.BLASTER);
            pick_period++;
        } else if (pick_period == 2) {
            world.pickHero(HeroName.GUARDIAN);
            pick_period++;
        } else {
            //if (extraSentry) {
            //    world.pickHero(HeroName.SENTRY);
            //} else {
                world.pickHero(HeroName.BLASTER);
            //}
            pick_period = 0;
        }
    }

    private Boolean isOccupied(Cell cell, World world) {
        for (int i = 0; i < world.getMyHeroes().length; i++) {
            if (world.getMyHeroes()[i].getCurrentHP() >= 0) {
                if (world.getMyHeroes()[i].getCurrentCell().equals(cell)) {
                    return true;
                }
            }
        }
        return false;
    }
    public void moveTurn(World world) {
        System.out.println("move started");
        Hero My_hero = null;
        Hero[] heroes = world.getMyHeroes();
        System.out.println("Phase in move:" + world.getMovePhaseNum());
        Blocked_Cells.clear();
        for (int j = 0; j < world.getMyHeroes().length; j++) 
        {
            if(world.getMyHeroes()[j].getCurrentHP() >0 )
                Blocked_Cells.add(world.getMyHeroes()[j].getCurrentCell());
        }
        for (int i = 0; i < 4; i++) 
        {
            My_hero = world.getMyHeroes()[i];
            if(My_hero.getCurrentHP() <= 0)
                continue;
            Cell target = getClosestCells(world, My_hero);
            finding_good_cell_to_move(heroes,My_hero, world);
            NiceCell niceCell;
            niceCell = niceCells.poll();
            //System.out.println("size of niceCells is: " + niceCells.size());
            //System.out.println("here in nice cell: " + niceCell.hero);
            //System.out.println("direction for hero to move in nice cell: " + niceCell.direction);
            //System.out.println("turn:" + world.getCurrentTurn() + ",id:" + My_hero.getId() +
            //        ",row:" + niceCell.target.getRow() + ",col:" + niceCell.target.getColumn());
            
            if (!Arrays.asList(world.getMyDeadHeroes()).contains(My_hero)
                    && world.getAP() >= My_hero.getMoveAPCost()) {
                Blocked_Cells.remove(My_hero.getCurrentCell());
                Blocked_Cells.add(niceCell.target);
            }
            if(niceCell.direction != null)
                world.moveHero(niceCell.hero, niceCell.direction);
            niceCells.clear();
         }

        myHeroesHp.put(My_hero, My_hero.getCurrentHP());
        Blocked_Cells.clear();
    }


    public void actionTurn(World world) {
        System.out.println("action started : My score="+world.getMyScore()+ " Opp score=" +world.getOppScore());
        Hero[] heroes = world.getMyHeroes();
        ArrayList<Cell> Opp_cells = new ArrayList<>();
        Hero[] Opp_Heroes = world.getOppHeroes();

        for (int i = 0; i < 4; i++) {
            if (Opp_Heroes[i].getCurrentCell().isInVision()) {
                Opp_cells.add(Opp_Heroes[i].getCurrentCell());
            }
        }
        HashMap<Hero, Integer> healths = new HashMap<>();
        for(Hero uHero : Opp_Heroes)
            healths.put(uHero, uHero.getCurrentHP());
        
        for (Hero hero : heroes) {
            boolean flag = true;
            for (int i = 0; i < world.getOppHeroes().length; i++) {
                if (world.getOppHeroes()[i].getName().equals(HeroName.GUARDIAN)
                        && world.getOppHeroes()[i].getCurrentHP() > 100
                        && world.manhattanDistance(hero.getCurrentCell(), world.getOppHeroes()[i].getCurrentCell()) < 3) {
                    // go to dodge and escape!
                    flag = false;
                }
            }
            Cell hero_cell = hero.getCurrentCell();
            if (flag) {
                if (hero.getName().equals(HeroName.SENTRY)) {

                    if (hero.getAbility(AbilityName.SENTRY_RAY).isReady()) {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if(!oppHero.getCurrentCell().isInVision()|| healths.get(oppHero) <=0)
                                continue;
                            if (world.isInVision(hero_cell, oppHero.getCurrentCell())) { 
                                if (healths.get(oppHero) < minHP) {
                                    goodOpp = oppHero;
                                    minHP = healths.get(oppHero);
                                }
                            }
                        }
                        if (goodOpp != null)
                        {
                            world.castAbility(hero, AbilityName.SENTRY_RAY, goodOpp.getCurrentCell());
                            healths.put(goodOpp, healths.get(goodOpp) - 50);
                        }
                    } 
                    else if(hero.getAbility(AbilityName.SENTRY_ATTACK).isReady())
                    {
                        int minHP = 1000;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) 
                        {
                            if(!oppHero.getCurrentCell().isInVision()|| healths.get(oppHero) <=0)
                                continue;
                            if (hero.getAbility(AbilityName.SENTRY_ATTACK).isReady()
                                    && world.isInVision(hero_cell, oppHero.getCurrentCell())) {
                                if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                        <= hero.getAbility(AbilityName.SENTRY_ATTACK).getRange()
                                        + hero.getAbility(AbilityName.SENTRY_ATTACK).getAreaOfEffect()) {
                                    
                                    if (healths.get(oppHero) < minHP) {
                                        goodOpp = oppHero;                                       
                                        minHP = healths.get(oppHero);
                                    }
                                }
                            }
                        }
                        if (goodOpp != null)
                        {
                            world.castAbility(hero, AbilityName.SENTRY_ATTACK, goodOpp.getCurrentCell());
                            healths.put(goodOpp, healths.get(goodOpp) - 30);
                        }
                    }

                } else if (hero.getName().equals(HeroName.BLASTER)) {
                    
                    if (hero.getAbility(AbilityName.BLASTER_BOMB).isReady()) {
                        Ability ability = hero.getAbility(AbilityName.BLASTER_BOMB);
                        int bord = ability.getRange();
                        int area = ability.getAreaOfEffect();
                        Cell goodCell = null;
                        ArrayList<Hero> list = new ArrayList<>();
                        double maxScore = -1000;
                        for(Hero oppHero : Opp_Heroes)
                        {
                            if(oppHero.getCurrentCell().isInVision()==false
                                    || world.manhattanDistance(oppHero.getCurrentCell(), hero.getCurrentCell()) > bord + area)
                                continue;
                            int heroRow = oppHero.getCurrentCell().getRow();
                            int heroCol = oppHero.getCurrentCell().getColumn();
                            for(int i=-area; i<= area; i++)
                            {
                                for(int j=Math.abs(i)- area; j<= Math.abs(i)+ area; j++)
                                {
                                    if(world.manhattanDistance(hero.getCurrentCell(), world.getMap().getCell(heroRow+i, heroCol +j)) > bord)
                                        continue;
                                    double score = getCellScoreForAction(world,hero,ability,Opp_Heroes,healths,list,maxScore);
                                    if(score > maxScore)
                                    {
                                        maxScore = score;
                                        goodCell = world.getMap().getCell(heroRow+i, heroCol +j);
                                    }
                                }
                            }
                        }
                        if (goodCell != null)
                        {
                            if(world.getAP() >= ability.getAPCost())
                            {
                                world.castAbility(hero, ability, goodCell);
                                for(Hero aHero : list)
                                {
                                    healths.put(aHero,healths.get(aHero)-40);
                                }
                            }
                            //40 kam
                        }
                    } else if (hero.getAbility(AbilityName.BLASTER_ATTACK).isReady()) {
                        Ability ability = hero.getAbility(AbilityName.BLASTER_ATTACK);
                        int bord = ability.getRange();
                        int area = ability.getAreaOfEffect();
                        ArrayList<Hero> list = new ArrayList<>();
                        Cell goodCell = null;
                        double maxScore = -1000;
                        for(Hero oppHero : Opp_Heroes)
                        {
                            if(oppHero.getCurrentCell().isInVision()==false
                                    || world.manhattanDistance(oppHero.getCurrentCell(), hero.getCurrentCell()) > bord + area)
                                continue;
                            int heroRow = oppHero.getCurrentCell().getRow();
                            int heroCol = oppHero.getCurrentCell().getColumn();
                            for(int i=-area; i<= area; i++)
                            {
                                for(int j=Math.abs(i)- area; j<= Math.abs(i)+ area; j++)
                                {
                                    if(world.manhattanDistance(hero.getCurrentCell(), world.getMap().getCell(heroRow+i, heroCol +j)) > bord)
                                        continue;
                                    double score = getCellScoreForAction(world,hero,ability,Opp_Heroes,healths,list,maxScore);
                                    if(score > maxScore)
                                    {
                                        maxScore = score;
                                        goodCell = world.getMap().getCell(heroRow+i, heroCol +j);
                                    }
                                }
                            }
                        }
                        if (goodCell != null)
                        {
                            if(world.getAP() >= ability.getAPCost())
                            {
                                world.castAbility(hero, ability, goodCell);
                                for(Hero aHero : list)
                                {
                                    healths.put(aHero,healths.get(aHero)-20);
                                }
                            }
                            //20 kam
                        }
                    }
                } else if (hero.getName().equals(HeroName.GUARDIAN)) {
                    if (hero.getAbility(AbilityName.GUARDIAN_ATTACK).isReady()) {
                        Ability ability = hero.getAbility(AbilityName.GUARDIAN_ATTACK);
                        for (Cell Opp_cell : Opp_cells) {
                            if (!Opp_cell.isInVision()) {
                                continue;
                            }
                            if (world.manhattanDistance(hero_cell, Opp_cell) <=
                                    ability.getRange()) {
                                if(world.getAP() >= ability.getAPCost())
                                {
                                    world.castAbility(hero, AbilityName.GUARDIAN_ATTACK, Opp_cell);
                                    healths.put(world.getOppHero(Opp_cell), healths.get(world.getOppHero(Opp_cell)) - 40);

                                }
                                break;
                            }
                        }
                    }
                    if (hero.getAbility(AbilityName.GUARDIAN_FORTIFY).isReady()) {
                        for (Hero hero1 : world.getMyHeroes()) {
                            Cell des = hero1.getCurrentCell();
                            if (hero1.getCurrentHP()>0 && hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                    hero.getAbility(AbilityName.GUARDIAN_FORTIFY).getRange()) {
                                world.castAbility(hero, AbilityName.GUARDIAN_FORTIFY, des);
                                break;
                            }
                        }
                    }
                } else {
                    // Healer
                    if (hero.getAbility(AbilityName.HEALER_ATTACK).isReady()) {
                        int minHP = 100;
                        Hero goodOpp = null;
                        for (Hero oppHero : Opp_Heroes) {
                            if(!oppHero.getCurrentCell().isInVision()|| healths.get(oppHero) <=0)
                                continue;
                            if (world.manhattanDistance(hero_cell, oppHero.getCurrentCell())
                                    - hero.getAbility(AbilityName.HEALER_ATTACK).getAreaOfEffect() <=
                                    hero.getAbility(AbilityName.HEALER_ATTACK).getRange()) {
                                
                                if (healths.get(oppHero) < minHP) {
                                    minHP = healths.get(oppHero);
                                    goodOpp = oppHero;
                                }
                            }
                        }
                        if (goodOpp != null)
                        {
                            world.castAbility(hero, AbilityName.HEALER_ATTACK, goodOpp.getCurrentCell());
                            healths.put(goodOpp, healths.get(goodOpp) - 25);
                        }
                    }
                    if (hero.getAbility(AbilityName.HEALER_HEAL).isReady()) {
                        for (Hero hero1 : world.getMyHeroes()) {
                            Cell des = hero1.getCurrentCell();
                            if (hero1.getCurrentHP() > 0 && hero1.getCurrentHP() < hero1.getMaxHP() && world.manhattanDistance(hero_cell, des) <=
                                    hero.getAbility(AbilityName.HEALER_HEAL).getRange()) {
                                world.castAbility(hero, AbilityName.HEALER_HEAL, des);
                                break;
                            }
                        }
                    }
                }
            }
            else if (!hero.getCurrentCell().isInObjectiveZone() && world.getAP() > 0) //dodge 
            {
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

    private Cell change(Direction direction, World world, int row, int column) {
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

}