package com.practicaldime.jetty.gql.dao;

import com.coxautodev.graphql.tools.GraphQLResolver;
import com.practicaldime.jetty.gql.model.Link;
import com.practicaldime.jetty.gql.model.User;

public class LinkResolver implements GraphQLResolver<Link> {
    
    private final UserRepository userRepository;

    public LinkResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User postedBy(Link link) {
        if (link.getUserId() == null) {
            return null;
        }
        return userRepository.findById(link.getUserId());
    }
}