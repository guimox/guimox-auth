package com.guimox.auth.repository;

import com.guimox.auth.model.App;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppRepository extends CrudRepository<App, Long> {
    Optional<App> findByName(String app);
}