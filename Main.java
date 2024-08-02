import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

// Player 클래스: 플레이어 정보를 나타내는 클래스
class Player {
    private String name;
    private char team;
    private int health;
    private int score;
    private Money money; // 플레이어의 자금
    private Weapon weapon; // 플레이어의 무기

    public Player(String name, char team) {
        this.name = name;
        this.team = team;
        this.health = 100; // 기본 체력 설정
        this.score = 0; // 초기 점수
        this.money = new Money(5000); // 초기 자금 설정
    }

    // 게터 및 세터 메서드
    public String getName() {
        return name;
    }

    public char getTeam() {
        return team;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Money getMoney() {
        return money;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    // 플레이어 정보 출력 메서드
    public void displayInfo() {
        System.out.println("이름: " + name);
        System.out.println("팀: " + (team == 'T' ? "테러리스트" : "대테러리스트"));
        System.out.println("체력: " + health);
        System.out.println("점수: " + score);
        System.out.println("자금: $" + money.getBalance());
        if (weapon != null) {
            System.out.println("무기: " + weapon.getName());
        } else {
            System.out.println("무기: 없음");
        }
    }
}

// Weapon 클래스: 무기 정보를 나타내는 클래스
class Weapon {
    private String name;
    private int damage;
    private int headshotDamage;
    private int price;

    public Weapon(String name, int damage, int headshotDamage, int price) {
        this.name = name;
        this.damage = damage;
        this.headshotDamage = headshotDamage;
        this.price = price;
    }

    // 게터 메서드
    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    public int getHeadshotDamage() {
        return headshotDamage;
    }

    public int getPrice() {
        return price;
    }
}

// Attack 클래스: 공격 기능을 담당하는 클래스
class Attack {
    public static void perform(Player attacker, Player target, boolean isHeadshot, BombPlanting bombPlanting, Round round) {
        if (attacker.getTeam() == target.getTeam()) {
            System.out.println("같은 팀을 공격할 수 없습니다!");
            return;
        }

        if (target.getHealth() <= 0) {
            System.out.println("이미 사망한 플레이어입니다!");
            return;
        }

        Weapon weapon = attacker.getWeapon();
        int damage = isHeadshot ? weapon.getHeadshotDamage() : weapon.getDamage();

        target.setHealth(target.getHealth() - damage);
        System.out.println(attacker.getName() + "이(가) " + target.getName() + "을(를) " + weapon.getName() + "(으)로 " + damage + " 데미지로 공격했습니다!");

        // 공격 시 타이머 단축
        if (bombPlanting.isBombPlanted()) {
            Random random = new Random();
            int reducedTime = 15 + random.nextInt(6); // 15 ~ 20초 랜덤 단축
            bombPlanting.reduceBombTime(reducedTime * 1000);
            System.out.println("폭탄 타이머가 " + reducedTime + "초 단축되었습니다!");
        }

        if (target.getHealth() <= 0) {
            System.out.println(target.getName() + "이(가) 죽었습니다!");
            attacker.getMoney().add(300); // 자금 추가
            attacker.setScore(attacker.getScore() + 1); // 점수 추가
            System.out.println(attacker.getName() + "이(가) 자금 $300원과 점수 1점을 획득했습니다!");

            if (round.checkRoundEnd()) {
                round.endRound();
            }
        }
    }
}

// BombPlanting 클래스: 폭탄 설치 및 해체를 담당하는 클래스
class BombPlanting {
    private boolean bombPlanted;
    private boolean bombDefused;
    private Timer bombTimer;
    private long bombTimeRemaining;
    private Round round;

    public BombPlanting(Round round) {
        this.bombPlanted = false;
        this.bombDefused = false;
        this.bombTimeRemaining = 0;
        this.round = round;
    }

    public void plantBomb() {
        if (!bombPlanted) {
            bombPlanted = true;
            bombDefused = false;
            bombTimeRemaining = 2 * 60 * 1000; // 2분 설정
            System.out.println("폭탄이 설치되었습니다!");

            bombTimer = new Timer();
            bombTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    bombTimeRemaining -= 1000;
                    if (bombTimeRemaining <= 0) {
                        bombTimer.cancel();
                        bombTimer.purge();
                        if (!bombDefused) {
                            System.out.println("폭탄이 폭발했습니다! 테러리스트 승리!");
                            round.distributeRewards('T');
                            round.endRound();
                        }
                    }
                }
            }, 0, 1000);
        } else {
            System.out.println("이미 폭탄이 설치되었습니다!");
        }
    }

    public void defuseBomb() {
        if (bombPlanted && !bombDefused) {
            bombDefused = true;
            bombTimer.cancel();
            bombTimer.purge();
            System.out.println("폭탄이 해체되었습니다! 대테러리스트 승리!");
            round.distributeRewards('C');
            round.endRound();
        } else {
            System.out.println("설치된 폭탄이 없습니다!");
        }
    }

    public void reduceBombTime(long time) {
        bombTimeRemaining -= time;
        if (bombTimeRemaining < 0) {
            bombTimeRemaining = 0;
        }
    }

    public boolean isBombPlanted() {
        return bombPlanted;
    }

    public boolean isBombDefused() {
        return bombDefused;
    }

    public long getBombTimeRemaining() {
        return bombTimeRemaining;
    }
}

// Round 클래스: 라운드 진행을 담당하는 클래스
class Round {
    private Player[] terrorists;
    private Player[] counterTerrorists;
    private BombPlanting bombPlanting;

    public Round(Player[] terrorists, Player[] counterTerrorists) {
        this.terrorists = terrorists;
        this.counterTerrorists = counterTerrorists;
        this.bombPlanting = new BombPlanting(this);
    }

    public void startRound() {
        System.out.println("라운드가 시작되었습니다!");
    }

    public void distributeRewards(char winningTeam) {
        Player[] winningPlayers = (winningTeam == 'T') ? terrorists : counterTerrorists;
        Player[] losingPlayers = (winningTeam == 'T') ? counterTerrorists : terrorists;

        for (Player player : winningPlayers) {
            player.getMoney().add(3000); // 승자팀 자금 추가
        }

        for (Player player : losingPlayers) {
            player.getMoney().add(1900); // 패자팀 자금 추가
        }

        System.out.println((winningTeam == 'T' ? "테러리스트" : "대테러리스트") + " 팀이 라운드에서 승리하였습니다!");
        System.out.println("패자팀에게는 자금 $1900원이 지급되었습니다.");
        System.out.println("승자팀에게는 자금 $3000원이 지급되었습니다.");
    }

    public boolean checkRoundEnd() {
        int remainingTerrorists = 0;
        int remainingCounterTerrorists = 0;

        for (Player player : terrorists) {
            if (player.getHealth() > 0) {
                remainingTerrorists++;
            }
        }

        for (Player player : counterTerrorists) {
            if (player.getHealth() > 0) {
                remainingCounterTerrorists++;
            }
        }

        if (bombPlanting.isBombPlanted() && remainingCounterTerrorists == 0) {
            System.out.println("대테러리스트가 전멸하였습니다. 폭탄이 폭발합니다!");
            bombPlanting.reduceBombTime(bombPlanting.getBombTimeRemaining());
        } else if (!bombPlanting.isBombPlanted() && remainingTerrorists == 0) {
            System.out.println("테러리스트가 전멸하였습니다. 대테러리스트 승리!");
            distributeRewards('C');
            return true;
        } else if (!bombPlanting.isBombPlanted() && remainingCounterTerrorists == 0) {
            System.out.println("대테러리스트가 전멸하였습니다. 테러리스트 승리!");
            distributeRewards('T');
            return true;
        }

        return false;
    }

    public void endRound() {
        System.out.println("라운드가 종료되었습니다!");
        System.exit(0);
    }

    public BombPlanting getBombPlanting() {
        return bombPlanting;
    }
   
}

// Map 클래스: 맵 정보를 관리하는 클래스
class Map {
    private static final String[] maps = {"de_dust2", "overpass", "mirage"};
    private String currentMap;

    public Map() {
        Random random = new Random();
        currentMap = maps[random.nextInt(maps.length)];
    }

    public String getCurrentMap() {
        return currentMap;
    }
}

// Money 클래스: 플레이어의 자금을 관리하는 클래스
class Money {
    private int balance;

    public Money(int balance) {
        this.balance = balance;
    }

    public int getBalance() {
        return balance;
    }

    public void add(int amount) {
        balance += amount;
    }

    public void subtract(int amount) {
        balance -= amount;
    }
}

// Main 클래스: 프로그램의 시작점
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 맵 선택
        Map map = new Map();
        System.out.println("선택된 맵: " + map.getCurrentMap());

        // 플레이어 10명 생성 및 팀 분류
        Player[] players = new Player[10];
        Player[] terrorists = new Player[5];
        Player[] counterTerrorists = new Player[5];

        for (int i = 0; i < 10; i++) {
            char team = i < 5 ? 'T' : 'C';
            players[i] = new Player("Player " + (char) ('a' + i), team);
            if (i < 5) {
                terrorists[i] = players[i];
            } else {
                counterTerrorists[i - 5] = players[i];
            }
        }

        // 무기 생성
        Weapon ak47 = new Weapon("AK-47", 40, 100, 3000); // 헤드샷 데미지 100으로 수정
        Weapon awp = new Weapon("AWP", 80, 150, 6000);
        Weapon smg = new Weapon("SMG", 25, 70, 2000); // SMG 무기 추가
        Weapon glock = new Weapon("Glock", 20, 50, 0); // 테러리스트 기본 무기
        Weapon usp = new Weapon("USP", 25, 55, 0); // 대테러리스트 기본 무기

        // 게임 생성 및 시작
        Round round = new Round(terrorists, counterTerrorists);
        round.startRound();

        // 플레이어별 무기 구매
        for (Player player : players) {
            while (true) {
                System.out.println(player.getName() + "의 잔액: $" + player.getMoney().getBalance());
                System.out.println("사용 가능한 무기:");
                System.out.println("1. AK-47 - $3000");
                System.out.println("2. AWP - $6000");
                System.out.println("3. SMG - $2000"); // SMG 무기 추가
                System.out.println("4. 무기 미구매 (기본 무기 지급) - $0");
                System.out.print("무기를 선택하세요 (1, 2, 3 또는 4): ");
                int choice = scanner.nextInt();
                if (choice == 1) {
                    if (player.getMoney().getBalance() >= ak47.getPrice()) {
                        player.setWeapon(ak47);
                        player.getMoney().subtract(ak47.getPrice());
                        System.out.println("AK-47 구매 완료!");
                        break;
                    } else {
                        System.out.println("AK-47을 구매할 자금이 부족합니다!");
                    }
                } else if (choice == 2) {
                    if (player.getMoney().getBalance() >= awp.getPrice()) {
                        player.setWeapon(awp);
                        player.getMoney().subtract(awp.getPrice());
                        System.out.println("AWP 구매 완료!");
                        break;
                    } else {
                        System.out.println("AWP를 구매할 자금이 부족합니다!");
                    }
                } else if (choice == 3) {
                    if (player.getMoney().getBalance() >= smg.getPrice()) {
                        player.setWeapon(smg);
                        player.getMoney().subtract(smg.getPrice());
                        System.out.println("SMG 구매 완료!");
                        break;
                    } else {
                        System.out.println("SMG를 구매할 자금이 부족합니다!");
                    }
                } else if (choice == 4) {
                    if (player.getTeam() == 'T') {
                        player.setWeapon(glock);
                        System.out.println("Glock 지급 완료!");
                    } else {
                        player.setWeapon(usp);
                        System.out.println("USP 지급 완료!");
                    }
                    break;
                } else {
                    System.out.println("잘못된 입력입니다. 다시 입력하세요.");
                }
            }
        }

        // 라운드 진행
        while (true) {
            System.out.println("1. 공격");
            System.out.println("2. 폭탄 설치");
            System.out.println("3. 폭탄 해체");
            System.out.println("4. 팀별 남은 플레이어 보기");
            System.out.println("5. 모든 플레이어 정보 보기");
            System.out.print("행동을 선택하세요 (1, 2, 3, 4 또는 5): ");
            int action = scanner.nextInt();
            if (action == 1) {
                // 공격 진행
                System.out.print("공격할 플레이어 번호를 입력하세요 (1-10): ");
                int attackerIndex = scanner.nextInt() - 1;
                System.out.print("타겟 플레이어 번호를 입력하세요 (1-10): ");
                int targetIndex = scanner.nextInt() - 1;
                System.out.print("공격 유형을 선택하세요 (1. 헤드샷, 2. 몸샷): ");
                int attackType = scanner.nextInt();
                boolean isHeadshot = (attackType == 1);
                Attack.perform(players[attackerIndex], players[targetIndex], isHeadshot, round.getBombPlanting(), round);
            } else if (action == 2) {
                // 폭탄 설치 진행
                round.getBombPlanting().plantBomb();
            } else if (action == 3) {
                // 폭탄 해체 진행
                round.getBombPlanting().defuseBomb();
            } else if (action == 4) {
                // 팀별 남은 플레이어 이름 보기
                System.out.print("테러리스트 팀 남은 플레이어: ");
                for (Player player : terrorists) {
                    if (player.getHealth() > 0) {
                        System.out.print(player.getName() + " ");
                    }
                }
                System.out.println();

                System.out.print("대테러리스트 팀 남은 플레이어: ");
                for (Player player : counterTerrorists) {
                    if (player.getHealth() > 0) {
                        System.out.print(player.getName() + " ");
                    }
                }
                System.out.println();
            } else if (action == 5) {
                // 모든 플레이어 정보 보기
                for (Player player : players) {
                    player.displayInfo();
                    System.out.println();
                }
            }

            // 폭탄이 설치된 경우 남은 시간 표시
            if (round.getBombPlanting().isBombPlanted()) {
                long timeRemaining = round.getBombPlanting().getBombTimeRemaining();
                long seconds = (timeRemaining / 1000) % 60;
                long minutes = (timeRemaining / (1000 * 60)) % 60;
                System.out.println("폭탄이 폭발까지 남은 시간: " + minutes + "분 " + seconds + "초");
            }

            // 라운드 종료 조건 체크
            if (round.checkRoundEnd()) {
                round.endRound();
                break;
            }
        }
        scanner.close();
    }
}