#!/bin/bash

echo "========================================================"
echo "Prime App Database Repair Utility"
echo "========================================================"
echo ""
echo "This script will help diagnose and fix database issues."
echo ""

function show_menu {
    echo "Choose an operation:"
    echo "1. Check database connection"
    echo "2. Repair Flyway migration state"
    echo "3. Fix potential username column issues"
    echo "4. Reset database completely (DANGEROUS)"
    echo "5. Exit"
    echo ""
    read -p "Enter your choice (1-5): " choice
    
    case $choice in
        1) check_connection ;;
        2) repair_flyway ;;
        3) fix_username ;;
        4) reset_database ;;
        5) exit 0 ;;
        *) show_menu ;;
    esac
}

function check_connection {
    echo ""
    echo "Checking MySQL connection..."
    if mysql -u root -e "SELECT 'Connection successful!' as Status;" &> /dev/null; then
        echo "MySQL connection successful."
        echo "Checking if database exists..."
        if mysql -u root -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'prime_app_db';" | grep -q prime_app_db; then
            echo "Database 'prime_app_db' exists."
        else
            echo "Database 'prime_app_db' does not exist. Creating it..."
            mysql -u root -e "CREATE DATABASE IF NOT EXISTS prime_app_db;"
            echo "Database created."
        fi
    else
        echo "Database connection failed. Please check that MySQL is running"
        echo "and that your credentials are correct."
    fi
    echo ""
    show_menu
}

function repair_flyway {
    echo ""
    echo "Running Flyway repair to fix migration history..."
    ./mvnw flyway:repair -Dflyway.configFiles=src/main/resources/application.properties
    echo "Repair completed."
    echo ""
    show_menu
}

function fix_username {
    echo ""
    echo "Checking for username column issues..."
    if mysql -u root prime_app_db -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'prime_app_db' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'username';" | grep -q -E '^[1-9]'; then
        echo "Username column exists, checking if it's properly set..."
        if mysql -u root prime_app_db -e "SELECT COUNT(*) FROM users WHERE username IS NULL OR username = '';" | grep -q -E '^[1-9]'; then
            echo "Some users have empty usernames. Updating from email addresses..."
            mysql -u root prime_app_db -e "UPDATE users SET username = email WHERE username IS NULL OR username = '';"
            echo "Users updated with usernames from their email addresses."
        else
            echo "All usernames are properly set."
        fi
    else
        echo "Username column not found in users table. This is a problem."
        echo "This will be fixed by migrations, but you may need to reset your database."
    fi
    echo ""
    show_menu
}

function reset_database {
    echo "========================================================"
    echo "WARNING: This will COMPLETELY RESET your database!"
    echo "ALL DATA WILL BE LOST! This cannot be undone!"
    echo "========================================================"
    echo ""
    read -p "Type 'CONFIRM' to proceed (or anything else to cancel): " confirm
    
    if [ "$confirm" != "CONFIRM" ]; then
        show_menu
        return
    fi
    
    echo ""
    echo "Connecting to MySQL to reset the database..."
    mysql -u root -e "DROP DATABASE IF EXISTS prime_app_db; CREATE DATABASE prime_app_db;"
    echo ""
    echo "Database has been completely reset. All migrations will run from scratch"
    echo "when you next start the application."
    echo ""
    show_menu
}

# Make the script executable
chmod +x ./mvnw

# Start the menu
show_menu 