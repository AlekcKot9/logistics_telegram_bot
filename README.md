# 🤖 Logistics Telegram Bot

Spring Boot приложение для управления логистикой через Telegram бота.

## 📋 Описание

Telegram бот для управления логистическими операциями, включая:
- 👥 Регистрацию и авторизацию пользователей
- 📦 Создание и отслеживание заказов доставки
- 🚛 Управление транспортом
- 👨‍💼 Административные функции
- ⏰ Систему сессий с автоматическим завершением

## 🚀 Основные возможности

### 👤 Для пользователей
- 📝 Регистрация в системе
- 🔐 Авторизация по email
- 📦 Создание новых заказов доставки
- 📋 Просмотр истории своих заказов
- 👤 Управление профилем
- ⏰ Система сессий (30 минут активности)

### 👨‍💼 Для администраторов
- 📋 Просмотр всех заказов в системе
- 🚗 Управление парком транспорта
- ✏️ Изменение статусов заказов
- 🔄 Изменение статусов транспорта
- 🔐 Отдельная система авторизации администраторов

## 🏗 Архитектура проекта
```
src/main/java/com/logistics/
├── bot/
│   └── TelegramBot.java
├── service/
│   ├── OrderService.java
│   ├── OrderCreationService.java
│   ├── LoginService.java
│   ├── AuthService.java
│   ├── AdminService.java
│   ├── RegistrationService.java
│   ├── SessionService.java
│   └── MessageSender.java
├── model/
│   ├── Order.java
│   ├── Customer.java
│   ├── Vehicle.java
│   └── UserSession.java
└── session/
    └── UserSession.java
```


## 🛠 Технологии

- **Java 17+**
- **Spring Boot 3.x**
- **Telegram Bot API** (telegrambots-spring-boot-starter)
- **Spring Data JPA** (для работы с базой данных)
- **Maven** (система сборки)
- **Hibernate** (ORM)
- **PostgreSQL** (реляционная база данных)

## 📦 Зависимости

Основные зависимости проекта:

```xml
<dependencies>
    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots-spring-boot-starter</artifactId>
        <version>6.8.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```
## ⚙️ Конфигурация
Основные настройки в application.properties:
properties
## Telegram Bot Configuration
```
bot.token=${BOT_TOKEN:your_bot_token_here}
bot.username=${BOT_USERNAME:your_bot_username}

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/logistics
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:password}

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Session Configuration
session.timeout.minutes=30

# Server Configuration
server.port=8080
```
## Переменные окружения:
BOT_TOKEN - токен вашего Telegram бота от @BotFather

BOT_USERNAME - username бота (без @)

DATABASE_URL - URL базы данных PostgreSQL

DB_USERNAME - пользователь базы данных

DB_PASSWORD - пароль базы данных

## 🔧 Установка и запуск
1. Клонирование и настройка
git clone <repository-url>
cd logistics-bot
2. Настройка базы данных
sql
CREATE DATABASE logistics;
CREATE USER logistics_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE logistics TO logistics_user;
3. Настройка конфигурации
bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
4. Сборка приложения
bash
mvn clean package -DskipTests
5. Запуск приложения
```
java -jar target/logistics-bot-1.0.0.jar
```
## 🎮 Использование бота

### Основные команды:

| Команда | Описание | Доступ |
|---------|-----------|---------|
| `/start` 🚀 | Начать работу с ботом | Все |
| `/sign` 📝 | Регистрация в системе | Неавторизованные |
| `/login` 🔐 | Вход в систему | Неавторизованные |
| `/logout` 🚪 | Выход из системы | Авторизованные |
| `/new_order` 📦 | Создать новый заказ | Пользователи |
| `/my_orders` 📋 | Просмотр своих заказов | Пользователи |
| `/profile` 👤 | Просмотр профиля | Пользователи |
| `/admin` 👨‍💼 | Вход в режим администратора | Администраторы |
| `/help` ❓ | Помощь по командам | Все |

### Процесс работы:

#### 📝 Регистрация пользователя
1. **Ввод полного имени**
2. **Ввод email**
3. **Ввод телефона**
4. **Ввод адреса**

#### 📦 Создание заказа
1. **Ввод адреса доставки**
2. **Указание веса груза**
3. **Подтверждение данных**

#### 👨‍💼 Административные функции
1. **Просмотр всех заказов**
2. **Управление транспортом**
3. **Изменение статусов**
