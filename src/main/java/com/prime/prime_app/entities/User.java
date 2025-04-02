package com.prime.prime_app.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.prime.prime_app.entities.Attendance;
import com.prime.prime_app.entities.Performance;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Override
    public String getPassword() {
        return password;
    }

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String name;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "work_id", nullable = false, unique = true)
    private String workId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(name = "national_id", nullable = false, unique = true)
    private String nationalId;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_attempts")
    @Builder.Default
    private Integer loginAttempts = 0;

    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean credentialsNonExpired = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column
    private String region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<User> managedAgents = new HashSet<>();

    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ManagerAssignedAgent> managerAssignments = new HashSet<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Client> clients = new HashSet<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Attendance> attendanceRecords = new HashSet<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Performance> performanceRecords = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Set name as a combination of first and last name if not provided
        if (name == null && firstName != null && lastName != null) {
            name = firstName + " " + lastName;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Update name field if first or last name changes
        if (firstName != null && lastName != null) {
            name = firstName + " " + lastName;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null) {
            return new ArrayList<>();
        }
        return roles.stream()
                .filter(role -> role != null && role.getName() != null)
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    @Column(name = "is_agent_leader")
    @Builder.Default
    private boolean isAgentLeader = false;

    @Override
    public String getUsername() {
        return username;  // Use the username field for UserDetails
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Role.RoleType getPrimaryRole() {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream()
                .filter(role -> role != null)
                .findFirst()
                .map(Role::getName)
                .orElse(null);
    }
}