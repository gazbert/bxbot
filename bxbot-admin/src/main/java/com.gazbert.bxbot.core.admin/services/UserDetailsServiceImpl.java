/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.core.admin.services;

import com.gazbert.bxbot.core.admin.security.User;
import com.gazbert.bxbot.core.admin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * TODO Work in progress... not safe for production!
 *
 * User Details service for authenticating and authorizing users of the REST API.
 *
 * @author gazbert
 * @since 21/08/2016
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        final User user = userRepository.findByLoginId(username);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User with login_id %s does not exist!", username));
        }
        return new UserRepositoryUserDetails(user);
    }

    /**
     * Represents the User Details for a given user.
     */
    private final static class UserRepositoryUserDetails extends User implements UserDetails {

        private static final long serialVersionUID = 6394659348090857891L;

        private UserRepositoryUserDetails(User user) {
            super(user);
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return getRoles();
        }

        @Override
        public String getUsername() {
            return getLoginId();
        }

        // TODO What's the policy here?
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        // TODO What's the policy here?
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        // TODO What's the policy here?
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        // TODO What's the policy here?
        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
