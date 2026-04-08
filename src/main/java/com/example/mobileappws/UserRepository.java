package com.example.mobileappws;

import com.example.mobileappws.io.entity.UserEntity;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends PagingAndSortingRepository<UserEntity, Long>,
    CrudRepository<UserEntity, Long> {
    // query methods: the naming convention is fixed
    // Spring Data JPA is using the names to convert to SQL to query the DB
    UserEntity findByEmail(String email);
    UserEntity findByUserId(String userId);

}
