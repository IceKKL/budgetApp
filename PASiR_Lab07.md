Politechnika Krakowska im. Tadeusza Kościuszki

Wydział Mechaniczny

Katedra Informatyki Stosowanej M-7

Laboratorium: Finalizacja refactoryzacja i jakość projektu

Programowanie aplikacji

serwerowych

i rozproszonych Java

1  Cele laboratorium

Nr lab.: 07,

2 x 45 minut

dr hab inż. Grzegorz Filo

dr inż. Paweł Lempa

mgr inż. Konrad Wisowski

•  Przygotowanie pełnej konteneryzacji projektu

•  Podłączenie SonarCloud do GitHub i konfiguracja GitHub Actions do automatycznej

analizy jakości kodu.

•

 Refactoryzacja kodu

2  Wymagane oprogramowanie
•  Java Development Kit (JDK) 25

•  maven 3.9.x lub nowszy – należy się upewnić, że zmienne środowiskowe (M2_HOME oraz

PATH) są poprawnie ustawione

•  MySql 8.4.x – baza danych

•  Node.js 24.13 lub nowszy (LTS)- potrzebny do uruchomienia frontendu (React).

•  Git 2.53 lub nowszy

•

IDE: IntelliJ IDEA lub Visual Studio Code

o  dla IntelliJ IDEA - wtyczka .env files od JetBrains

•  Docker 4.62 lub nowszy

W przypadku wybrania Visual Studio Code, należy zainstalować następujące rozszerzenia:

o  Debugger for Java

o  Maven for Java

o  Spring Boot Dashboard

o  Language Support for Java™ by Red Hat

o

Java Test Runner

•  Przeglądarka Internetowa - dowolna wspierana, aktualna wersja (np. Chrome, Firefox, Edge).

•  Postman  -  narzędzie  do  testowania  API.  Ułatwia  wysyłanie  żądań  HTTP  oraz  analizę

odpowiedzi.

1

3  Połączenie frontendu i backendu w jedno repozytorium
3.1  Usunięcie repozytorium lokalnych z frontendu i backendu

Należy utworzyć nowy folder BudgetApp i w nim umieścić 2 foldery frontend i backend (ewentualnie
client i serwer – nazwy dowolne, ale intuicyjne), a w nich odpowiednie aplikacje. Następnie należy
usunąć repozytoria (Listing 3.1)

Listing 3.1 komendy do usunięcia repozytoriów lokalnych osobno z frontendu i backendu

Kolejny  krok  to  połączenie    .gitignore  z  obu  projektów  połączyć  w  jeden  na  poziomie  root
BudgetApp/.gitignore (Listing 3.2)

Listing 3.2 Połączone . gitignore w mono-repozytorium

Należy stworzyć  plik README.md w głównym katalogu projektu. Należy tam krótko opisać aplikację
i podać dane autora/ki.

Następnie

należy

zainicjalizować

już

połączone

repozytorium

lokalne

(Listing

3.3)

2

Listing 3.3 Komendy do inicjalizacji mono-repozytorium

4  Konfiguracja projektu w Dockerze
Należy uruchomoć Docker Desktop, następnie sprawdzić aktualne wersje dockeria i docker compose’a
(Listing 4.1).

Listing 4.1 Sprawdzenie wersji docker i docker compose

docker –version
docker compose version
docker-compose –version
W  tej  instrukcji  używa  się  aktualnej  składni  docker  compose.  Jeśli  działa  tylko  starsza  komenda
docker-compose, należy zamienić w poleceniach docker compose na docker-compose.

Ważne: komendy Docker Compose uruchamia się z katalogu głównego BudgetApp.

4.1  Plik docker-compose.yml

Pracę  należy  rozpocząć  o  stworzenie  w  głównym  katalogu  (tam  gdzie  znajdują  się  foldery  backend
i frontend)  pliku  docker-compose.yml.  Będzie  on  odpowiadał  za  stworzenie  poszczególnych
kontenerów (bazy danych, backend’u i frontend’u).

Rozpoczyna się go od przygotowania części bazodanowej znanej już z poprzednich laboratoriów (Listing
4.2).

Listing 4.2 docker-compose.yml część 1

services:
  mysql:
    image: mysql:8.4
    restart: unless-stopped
    env_file:
      - ./backend/.env
    ports:
      - "3306:3306"
    command:
      - mysqld
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/docker/initdb:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -u root -
p$${MYSQL_ROOT_PASSWORD} || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

3

A następnie nowych serwisów odpowiedzialnych za backend i frontend (Listing 4.3). Należy poprawnie
podać nazwę swojej bazy.

Listing 4.3docker-compose.yml część 2

  backend:
    build:
      context: ./backend
    restart: unless-stopped
    env_file:
      - ./backend/.env
    environment:
      SPRING_DATASOURCE_URL:
jdbc:mysql://mysql:3306/$${MYSQL_DATABASE}?allowPublicKeyRetrieval=true&u
seSSL=false&serverTimezone=UTC
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    build:
      context: ./frontend
    restart: unless-stopped
    ports:
      - "5174:8080"
    depends_on:
      - backend

volumes:
  mysql_data:

4

Najważniejsze elementy:

•  mysql uruchamia bazę MySQL 8.4 i zapisuje dane w wolumenie mysql_data
•  env_file: ./backend/.env ładuje hasła, nazwę bazy i sekret JWT z pliku .env
•  healthcheck sprawdza, czy MySQL jest gotowy do przyjmowania połączeń
•  backend buduje obraz z katalogu backend.
•  $${MYSQL_DATABASE} jest zapisane z podwójnym znakiem dolara celowo. Dzięki temu
Docker Compose nie podstawia tej wartości podczas czytania pliku, tylko przekazuje
`${MYSQL_DATABASE}` do kontenera, a Spring Boot rozwiązuje ją na podstawie zmiennych
z `ackend/.env

•  depends_on sprawia, że backend startuje dopiero po poprawnym uruchomieniu MySQL, a

frontend bo uruchomieniu backend’u

•  frontend buduje obraz z katalogu frontend i wystawia aplikację na porcie 5174.

4.2  Backend Dockerfile

Należy w BudgetApp/backend/ stworzyć plik Dockerfile (bez rozszerzeń). Ten plik () buduje
aplikację Spring Boot i uruchamia ją jako plik .jar. Używa budowania wieloetapowego: pierwszy etap
kompiluje projekt Mavenem, a drugi uruchamia gotową aplikację w mniejszym obrazie z JRE.

FROM maven:3.9-eclipse-temurin-25 AS build

Listing 4.4 Backend Dockerfile

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin appuser

COPY --from=build /app/target/*.jar app.jar

5

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]

Najważniejsze elementy:

•  maven:3.9-eclipse-temurin-25  zawiera  Maven  i  JDK  25  potrzebne  do  zbudowania

projektu

•  ./mvnw -DskipTests package buduje aplikację bez uruchamiania testów
•  eclipse-temurin:25-jre służy tylko do uruchomienia gotowego pliku .jar
•  useradd  ...  appuser  tworzy  nieuprzywilejowanego  użytkownika  do  uruchamiania

aplikacji

•  EXPOSE 8080 informuje, że backend działa na porcie 8080
•  USER appuser sprawia, że aplikacja nie działa jako root
•  ENTRYPOINT uruchamia aplikację komendą java -jar app.jar

4.3  Backend .dockerignore

Należy w BudgetApp/backend/ stworzyć plik .dockerignore (Listing 4.5).

Ten plik wyklucza z kontekstu budowania Dockera katalogi i pliki, które nie są potrzebne do zbudowania
backendu.  Dzięki  temu  obraz  buduje  się  szybciej  i  nie  kopiuje  niepotrzebnych  danych.  Należy
dostosować do swojego projektu.

Listing 4.5 Plik backend .dockerignore

target
.idea
.env

6

docker
docs
postman

Najważniejsze elementy:

•  target nie jest kopiowany, bo aplikacja jest budowana od nowa w kontenerze
•  .env nie trafia do obrazu, bo zawiera sekrety
•  docker  zawiera  konfigurację  starego  compose  dla  samej  bazy,  więc  nie  jest  potrzebny

w obrazie backendu

4.4  Frontend Dockerfile

Należy w BudgetApp/frontend/ stworzyć plik Dockerfile (Listing 4.6).

Ten plik buduje aplikację React/Vite i uruchamia ją przez nginx. Pierwszy etap instaluje zależności oraz
wykonuje build frontendu, a drugi etap kopiuje gotowe pliki statyczne do nginx.

Listing 4.6 Plik Dockerfile dla Frontend

FROM node:24-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY index.html ./
COPY tsconfig*.json ./
COPY vite.config.ts ./
COPY public public
COPY src src
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

7

Najważniejsze elementy:

•  node:24-alpine służy do zbudowania aplikacji React
•  npm ci instaluje zależności zgodnie z package-lock.json
•  Zamiast COPY .  kopiowane są tylko pliki potrzebne do zbudowania frontendu, co zmniejsza

ryzyko przypadkowego dodania poufnych danych do obrazu

•  npm run build tworzy produkcyjną wersję aplikacji w katalogu dist
•  nginxinc/nginx-unprivileged:1.27-alpine  serwuje  gotowe  pliki  statyczne  i  jest

przygotowany do działania bez użytkownika root

•  EXPOSE  8080  oznacza  port  wewnątrz  kontenera.  W  docker-compose.yml  jest  on

wystawiony na komputerze jako 5174

4.5  Frontend nginx.conf

Należy w BudgetApp/frontend/ stworzyć plik nginx.conf (Listing 4.7).

Listing 4.7 Plik nginx.conf

server {
    listen 8080;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}

8

Najważniejsze elementy:

•  listen 8080 uruchamia nginx na porcie 8080 wewnątrz kontenera. Porty poniżej 1024, np.

80, zwykle wymagają uprawnień użytkownika root

•  root /usr/share/nginx/html wskazuje katalog z plikami zbudowanego frontendu
•  try_files  $uri  $uri/  /index.html  sprawia, że ścieżki React  Routera, np.  /login,

działają także po odświeżeniu strony

4.6  Frontend .dockerignore

Należy w BudgetApp/frontend/ stworzyć plik .dockerignore (Listing 4.8).

Listing 4.8

node_modules
dist
.idea
*.log

Najważniejsze elementy:

•  node_modules nie jest kopiowany, bo zależności są instalowane w kontenerze przez npm ci
•  dist nie jest kopiowany, bo build jest tworzony od nowa w kontenerze
•  .idea i *.log są lokalnymi plikami środowiska programisty i nie są potrzebne w obrazie

4.7  Zbudowanie i uruchomienie aplikacji

Z katalogu głównego BudgetApp uruchom:

docker compose up -d --build

Co robi ta komenda:

•  pobiera obrazy bazowe, np. MySQL, Node, nginx, JDK
•  buduje obraz backendu
•  buduje obraz frontendu
•  uruchamia bazę MySQL

9

czeka, aż MySQL będzie gotowy

•
•  uruchamia backend
•  uruchamia frontend

Pierwsze uruchomienie może potrwać kilka minut, bo Docker pobiera zależności Mavena i npm.

4.8  Sprawdzenie statusu kontenerów

Aby sprawdzić, czy kontenery działają poprawnie, można użyć komendy:

docker compose ps

Powinny być widoczne trzy usługi:

•  mysql
•  backend
•
frontend

Przykładowe porty:

•  backend   0.0.0.0:8080->8080/tcp
•
frontend  0.0.0.0:5174->80/tcp
•  mysql     0.0.0.0:3306->3306/tcp

4.9  Wejście do aplikacji

Otwórz w przeglądarce (można m.in. kliknąć na link w Docker Desktop): http://localhost:5174

Frontend komunikuje się z backendem pod: http://localhost:8080

Backend komunikuje się z MySQL wewnątrz sieci Dockera przez nazwę usługi: mysql:3306

Dlatego w kontenerze backend nie łączy się z localhost:3306, tylko z mysql:3306.

4.10 Podgląd logów

Logi wszystkich usług: docker compose logs -f
Logi tylko backendu: docker compose logs -f backend
Logi tylko frontendu: docker compose logs -f frontend
Logi tylko MySQL: docker compose logs -f mysql

•
•
•
•
•  Wyjście z podglądu logów: Ctrl + C

4.11 Zatrzymanie aplikacji

Zatrzymanie kontenerów bez usuwania danych bazy:

docker compose down

Ponowne uruchomienie:

docker compose up -d

4.12 Pełne wyczyszczenie bazy danych

Jeśli chcesz usunąć kontenery razem z wolumenem MySQL, użyj:

docker compose down -v

Uwaga: ta komenda usuwa dane zapisane w bazie MySQL.

10

Po wyczyszczeniu można uruchomić aplikację od nowa:

docker compose up -d --build

4.13 Zakończenie

Należy upewnić, że aplikacja działa, wszystkie operacje są na niej wykonywane:

•  Zarejestrować dwóch użytkowników
•  Zalogować się na nich i dla każdego dodać przynajmniej jedną transakcję i sprawdzić czy

wyświetla się w liście

•  Dodać jedną grupę, dodać do niej drugiego użytkownika
•  Dodać kilka długów w grupie, zatwierdzić je sprawdzając na dwóch równocześnie

uruchomionych przeglądarkach (jedna w trybie incognito)

Jeżeli wszystko działa, należy wykonać commit i wysłać zmiany na zdalne repozutorium.

5  Podłączenie SonarCloud do GitHub
Aby  SonarCloude  zadziałał  poprawnie  w  darmowej  wersji,  należy  stworzyć  nowe  repozytorium  na
własnym koncie GitHub. Następnie należy dodać je jako drugie zdalne repozytorium do projektu i na
nie również wysłać wszystkie zmiany.

Po  utworzeniu  nowego  monorepozytorium  na  githubie  należy
https://sonarcloud.io i podłączyć swoje repozytorium (Rys. 5.1 Rys. 5.2 Rys. 5.4).

zarejestrować

się  na

11

Rys. 5.1 Ekran startowy strony sonarcloud

Rys. 5.2 Początkowa konfiguracja sonara

Następnie należy wybrać organizacje, i odpowiednie repozytorium (jeżeli nie jest to możliwe, należy
umieścić monorepo na swoim prywatnym GitHubie i wybrać je tutaj), następnie przejść do projektów
i stworzyć nowy (Rys. 5.3).

12

Rys. 5.3 Przejście do tworzenia nowego projektu na Sonar

Następnie należy wybrać odpowiednią organizację (Rys. 5.4Rys. 5.4 - 1), repozytorium (2) i nacisnąć
Set Up(3).

Po dodaniu projektu, sonar musi go przeanalizować (Rys. 5.5).

Rys. 5.4

13

Po analizie powinien pojawić się interaktywny raport (Rys. 5.6).

Rys. 5.5

Jeżeli się to nie uda zrobić, to należy ręcznie dodać ustawienia (Rys. 5.7).

Rys. 5.6

14

Rys. 5.7 Konfiguracja pipelinów sonara

Dalej  należy  postępować  zgodnie  z  instrukcją  sonara,  wybierając  jako  opcje  implementacji  github
actions. Po wstępnej konfiguracji sonar zasugeruje w jaki sposób powinna wyglądać zawartość pliku
yaml  dla mavena i web/js/ts. Obie treści należy połączyć ze sobą, aby sonar działał zarówno na
backend jak i frontend (Listing 5.1) w ścieżce .github/workflow/sonar.yml (Listing 5.3).

15

Listing 5.1 sonar.yml – plik konfiguracyjny pipelinów sonara

Kolejno trzeba stworzyć plik zawierający wszystkie potrzebne właściwości i ścieżki projektu, aby sonar
wiedział co i skąd ma pobierać (Listing 5.2). Należy podać swoje dane.

16

Listing 5.2 sonar-project.properties – plik z właściwościami sonara

Listing 5.3 WorkTree gotowego projektu

6  Bugfixing – Praca samodzielna
Należy dokonać niezbędnych poprawek tak aby sonar nie wykrył żadnych błędów zarówno po stronie
serwera jak i frontendu.

Listing 6.1 Widok github – github Actions – pipeline w trakcie sprawdzania

Listing 6.2 Widok github – github Actions – pipeline po poprawnym sprawdzeniu

7  Zakończenie przykładu

Należy pamiętać o wykonaniu PullRequest bez usuwania gałęzi i zaktualizować lokalną wersję

gałęzi main.

17

Należy spakować projekt przy użyciu komendy git (należy podać nazwę pliku wynikowego

i wykorzystywaną gałąź projektu):

git archive -o PASiR_Lab07_Nazwisko_Imię.zip main

Jeżeli to nie zadziała to należy projekt spakować przy użyciu rozszerzenia wtyczki Zipper nazwać
PASiR_Lab07_Nazwisko_Imię.zip  i  umieścić  na  delcie.  Należy  również  finalny  projekty  wysłać  na
GitHub classroom.

8  Źródła:

1.  https://www.jetbrains.com/idea
2.  https://www.jetbrains.com/help/idea/discover-intellij-idea.html
3.  https://spring.io/projects/spring-boot
4.  https://maven.apache.org/install.html
5.  https://www.mysql.com/

18

