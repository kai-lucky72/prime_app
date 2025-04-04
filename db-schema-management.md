# Database Schema Management with JPA

This project uses JPA/Hibernate schema management instead of Flyway for database schema evolution.

## How it works

The application uses Hibernate's automatic schema generation with the following configuration:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This setting tells Hibernate to:

- Keep existing tables
- Update table schemas if entity classes change
- Create new tables if they don't exist
- Never drop tables automatically

## Benefits of JPA Schema Management

- **Simplicity**: Schema is derived directly from your entity classes
- **Automatic Updates**: No need to write migration scripts for most changes
- **Integration**: Tight integration with your domain model

## Limitations

- **Limited Control**: Less precise control over schema changes than migration tools
- **No Versioning**: No explicit versioning of database changes
- **Data Migrations**: Cannot handle complex data migrations (only schema changes)

## Best Practices

1. **Back up your database** before deploying updates
2. **Test schema changes** in development/staging before production
3. For **complex changes**, consider manual SQL scripts
4. Use **proper entity constraints** and annotations to ensure correct schema generation

## Schema Generation Modes

You can use different values for `spring.jpa.hibernate.ddl-auto`:

- `update`: Update schema, preserving existing data (current setting)
- `validate`: Validate schema but don't make changes
- `create`: Drop and recreate schema on startup (CAUTION: destroys data)
- `create-drop`: Create schema on startup, drop on shutdown (for testing)
- `none`: Do nothing with the schema

For production environments, consider switching to `validate` mode after initial deployment.
