package com.guimox.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "apps")
@Getter
@Setter
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "apps")
    private Set<User> users;

    public App() {}

    public App(String name) {
        this.name = name;
    }
}
