// Memory table structure

package com.continuum.memory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "memories")
public class Memory {

    @Id
    @Column(nullable = false, updatable = false)
    public String id;

    @Column(nullable = false)
    public String userId;

    @Column(nullable = false)
    public String source;

    @Column(nullable = false, length = 4000)
    public String content;
}


