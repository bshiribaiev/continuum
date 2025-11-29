// User table structure

package com.continuum.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(nullable = false, updatable = false)
    public String id;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = true)
    public String displayName;
}


