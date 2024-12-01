import com.jcraft.jsch.*;
import java.io.*;
import java.util.Scanner;

public class wow {

    private static Session sshSession = null; // SSH 세션 객체

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        try {
            // 1. SSH 연결 설정 및 유지
            connectSSH();

            while (running) {
                System.out.println("\n환영합니다!");
                System.out.println("1. 조회");
                System.out.println("2. 관리");
                System.out.println("3. 종료");
                System.out.print("선택: ");
                int mainChoice = scanner.nextInt();

                switch (mainChoice) {
                    case 1:
                        showQueryMenu();
                        break;
                    case 2:
                        showManageMenu();
                        break;
                    case 3:
                        System.out.println("프로그램을 종료합니다.");
                        running = false;
                        break;
                    default:
                        System.out.println("잘못된 입력입니다. 다시 시도하세요.");
                }
            }
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 2. SSH 연결 종료
            closeSSH();
            System.out.println("프로그램 종료.");
        }
    }

    // 조회 메뉴
    public static void showQueryMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean queryRunning = true;

        while (queryRunning) {
            System.out.println("\n[조회 메뉴]");
            System.out.println("1. 부원 목록");
            System.out.println("2. 부원 정보");
            System.out.println("3. 행사 목록");
            System.out.println("4. 행사 정보");
            System.out.println("5. 출결");
            System.out.println("6. 이전 메뉴");
            System.out.print("선택: ");
            int queryChoice = scanner.nextInt();

            switch (queryChoice) {
                case 1:
                    executeQueryAndPrint("SELECT name FROM people;");
                    break;
                case 2:
                    executeQueryAndPrint("SELECT * FROM people;");
                    break;
                case 3:
                    executeQueryAndPrint("SELECT eventname FROM event;");
                    break;
                case 4:
                    executeQueryAndPrint("SELECT * FROM event;");
                    break;
                case 5:
                    executeQueryAndPrint("""
                        SELECT p.name, a.att, a.penalty,
                               CASE WHEN (a.att - a.penalty) >= 9 THEN '가능' ELSE '불가능' END AS 다음기수
                        FROM attendance a
                        JOIN people p ON a.id = p.id;
                        """);
                    break;
                case 6:
                    queryRunning = false;
                    break;
                default:
                    System.out.println("잘못된 입력입니다. 다시 시도하세요.");
            }
        }
    }

    // 관리 메뉴
    public static void showManageMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean manageRunning = true;

        while (manageRunning) {
            System.out.println("\n[관리 메뉴]");
            System.out.println("1. 부원 추가");
            System.out.println("2. 부원 수정");
            System.out.println("3. 부원 삭제");
            System.out.println("4. 행사 추가");
            System.out.println("5. 행사 수정");
            System.out.println("6. 행사 삭제");
            System.out.println("7. 출석 추가");
            System.out.println("8. 페널티 추가");
            System.out.println("9. 전체 정보 삭제");
            System.out.println("10. 이전 메뉴");
            System.out.print("선택: ");
            int manageChoice = scanner.nextInt();
            scanner.nextLine(); // 버퍼 비우기

            switch (manageChoice) {
                case 1: // 부원 추가
                    addMember();
                    break;
                case 2:
                    System.out.print("수정할 부원의 ID를 입력하세요: ");
                    String updateId = scanner.nextLine();
                    System.out.print("수정할 내용을 입력하세요 (예: name='홍길동', age=25): ");
                    String updateData = scanner.nextLine();
                    executeUpdate("UPDATE people SET " + updateData + " WHERE id='" + updateId + "';");
                    break;
                case 3:
                    System.out.print("삭제할 부원의 ID를 입력하세요: ");
                    String deleteId = scanner.nextLine();
                    executeUpdate("DELETE FROM people WHERE id='" + deleteId + "';");
                    break;
                case 4: // 행사 추가
                    addEvent();
                    break;
                case 5:
                    System.out.print("수정할 행사 이름을 입력하세요: ");
                    String updateEventName = scanner.nextLine();
                    System.out.print("수정할 내용을 입력하세요 (예: place='서울', cost=50000): ");
                    String updateEventData = scanner.nextLine();
                    executeUpdate("UPDATE event SET " + updateEventData + " WHERE eventname='" + updateEventName + "';");
                    break;
                case 6:
                    System.out.print("삭제할 행사 이름을 입력하세요: ");
                    String deleteEventName = scanner.nextLine();
                    executeUpdate("DELETE FROM event WHERE eventname='" + deleteEventName + "';");
                    break;
                case 7:
                    System.out.print("출석을 추가할 부원의 ID를 입력하세요: ");
                    String addAttendanceId = scanner.nextLine();
                    executeUpdate("UPDATE attendance SET att = att + 1 WHERE id='" + addAttendanceId + "';");
                    break;
                case 8:
                    System.out.print("페널티를 추가할 부원의 ID를 입력하세요: ");
                    String addPenaltyId = scanner.nextLine();
                    executeUpdate("UPDATE attendance SET penalty = penalty + 1 WHERE id='" + addPenaltyId + "';");
                    break;
                case 9:
                    executeUpdate("DELETE FROM people;");
                    executeUpdate("DELETE FROM attendance;");
                    executeUpdate("DELETE FROM event;");
                    System.out.println("모든 데이터가 삭제되었습니다.");
                    break;
                case 10:
                    manageRunning = false;
                    break;
                default:
                    System.out.println("잘못된 입력입니다. 다시 시도하세요.");
            }
        }
    }

    // 부원 추가
    private static void addMember() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("ID: ");
        String id = scanner.nextLine();
        System.out.print("이름: ");
        String name = scanner.nextLine();
        System.out.print("나이: ");
        String age = scanner.nextLine();
        System.out.print("전화번호: ");
        String phone = scanner.nextLine();
        System.out.print("학교: ");
        String school = scanner.nextLine();
        System.out.print("학번: ");
        String schoolnum = scanner.nextLine();
        System.out.print("피아노 시간: ");
        String pianotime = scanner.nextLine();
        System.out.print("거주지: ");
        String home = scanner.nextLine();
        System.out.print("기수: ");
        String grade = scanner.nextLine();

        // people 테이블에 새로운 사람 추가
        String queryPeople = String.format(
            "INSERT INTO people VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s');",
            id, name, age, phone, school, schoolnum, pianotime, home, grade
        );
        executeUpdate(queryPeople);

        // attendance 테이블에 해당 사람의 출결 정보 추가 (att = 0, penalty = 0)
        String queryAttendance = String.format(
            "INSERT INTO attendance VALUES ('%s', 0, 0);",
            id
        );
        executeUpdate(queryAttendance);
    }
    // 행사 추가
    private static void addEvent() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("장소: ");
        String place = scanner.nextLine();
        System.out.print("날짜 (YYYY-MM-DD): ");
        String eventdate = scanner.nextLine();
        System.out.print("비용: ");
        int cost = scanner.nextInt();
        scanner.nextLine(); // 버퍼 비우기
        System.out.print("행사 이름: ");
        String eventname = scanner.nextLine();

        String query = String.format(
            "INSERT INTO event VALUES ('%s', TO_DATE('%s', 'YYYY-MM-DD'), %d, '%s');",
            place, eventdate, cost, eventname
        );
        executeUpdate(query);
    }

    // SSH 연결 설정
    public static void connectSSH() throws JSchException {
        String sshHost = "cs.dongguk.edu"; // 학교 리눅스 서버 호스트
        int sshPort = 101;                // 학교 리눅스 서버 포트
        String sshUser = "2022113189";    // SSH 사용자 ID
        String sshPassword = "tonypass986!"; // SSH 비밀번호

        JSch jsch = new JSch();
        sshSession = jsch.getSession(sshUser, sshHost, sshPort);
        sshSession.setPassword(sshPassword);

        sshSession.setConfig("StrictHostKeyChecking", "no");
        sshSession.connect();
        System.out.println("SSH 연결 성공!");
    }

    // SQL 실행 공통 메서드
    private static void executeSQL(String query, boolean isQuery) {
        if (sshSession == null || !sshSession.isConnected()) {
            System.out.println("SSH 연결이 설정되지 않았습니다.");
            return;
        }

        String sqlCommand =
            "docker exec -i oracle-2022113189 sqlplus -s jaeyoon/tony0105@localhost:1521/XE <<EOF\n" +
            query + "\nEXIT;\nEOF";

        try {
            ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
            channel.setCommand(sqlCommand);

            InputStream in = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            if (isQuery) {
                System.out.println("\nSQL 실행 결과:");
            }
            while ((line = reader.readLine()) != null) {
                if (isQuery) {
                    System.out.println(line);
                }
            }

            reader.close();
            channel.disconnect();
        } catch (Exception e) {
            System.out.println("SQL 실행 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // SQL 실행 (업데이트용)
    public static void executeUpdate(String query) {
        executeSQL(query, true);
    }

    // SQL 명령 실행 및 결과 출력
    public static void executeQueryAndPrint(String query) {
        executeSQL(query, true);
    }

    // SSH 연결 종료
    public static void closeSSH() {
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
            System.out.println("SSH 연결 종료.");
        }
    }
}


