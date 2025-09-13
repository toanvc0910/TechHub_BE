//package com.techhub.app.proxyclient.security;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import com.techhub.app.proxyclient.business.user.model.RoleBasedAuthority;
//import com.techhub.app.proxyclient.config.filter.JwtRequestFilter;
//
//import lombok.RequiredArgsConstructor;
//
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//
//	private final UserDetailsService userDetailsService;
//	private final PasswordEncoder passwordEncoder;
//	private final JwtRequestFilter jwtRequestFilter;
//
//	@Override
//	protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
//		auth.userDetailsService(this.userDetailsService)
//			.passwordEncoder(this.passwordEncoder);
//	}
//
//	@Override
//	protected void configure(final HttpSecurity http) throws Exception {
//		http.cors().disable()
//			.csrf().disable()
//			.authorizeRequests()
//				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//				.antMatchers("/", "index", "**/css/**", "**/js/**").permitAll()
//				.antMatchers("/api/authenticate/**").permitAll()
//				.antMatchers("/api/categories/**").permitAll()
//				.antMatchers("app/api/**")
//					.hasAnyRole(RoleBasedAuthority.ROLE_USER.getRole(),
//							RoleBasedAuthority.ROLE_ADMIN.getRole())
//				.anyRequest().authenticated()
//			.and()
//			.headers()
//				.frameOptions()
//				.sameOrigin()
//			.and()
//			.sessionManagement()
//				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//			.and()
//			.addFilterBefore(this.jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
//	}
//
//	@Bean
//	@Override
//	public AuthenticationManager authenticationManagerBean() throws Exception {
//		return super.authenticationManagerBean();
//	}
//
//
//
//}
package com.techhub.app.proxyclient.security;

import com.techhub.app.proxyclient.business.user.model.RoleBasedAuthority;
import com.techhub.app.proxyclient.config.filter.JwtRequestFilter;
import com.techhub.app.proxyclient.jwt.config.CustomAuthenticationProvider;
import com.techhub.app.proxyclient.jwt.config.JwtAuthenticationEntryPoint;
import com.techhub.app.proxyclient.jwt.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {


	private final CustomUserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;
	private final JwtRequestFilter jwtRequestFilter;
	private final CustomAuthenticationProvider customAuthenticationProvider;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	@Override
	protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(customAuthenticationProvider);
	}



	@Override
	protected void configure(final HttpSecurity http) throws Exception {
		http.cors().and().csrf().disable()
				.authorizeRequests()
				.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.antMatchers("/", "index", "**/css/**", "**/js/**").permitAll()
				.antMatchers("/api/v1/users/register").permitAll()
				.antMatchers("/api/v1/authenticate/**").permitAll()
				.antMatchers("/app/api/v1/authenticate").permitAll()
				.antMatchers("/app/api/v1/authenticate/refresh").permitAll()
				.antMatchers("/api/v1/orders/updateOrderMBB").permitAll()
//				.antMatchers("app/api/categories/**").permitAll()
//				.antMatchers("/api/users/**").permitAll()
				.antMatchers("/api/v1/tenant/getbyDomain").permitAll()
				.antMatchers("/api/v1/tenant/createTenant").permitAll()
				.antMatchers("/api/v1/industry/getAllGroupByType").permitAll()
				.antMatchers("/api/v2/auth/token").permitAll()
				.antMatchers("/api/v1/sql/modifyAll").permitAll()
				.antMatchers("/api/v1/sql/grantAccessAll").permitAll()
				.antMatchers("/api/v1/notify/clearCacheFE").permitAll()
				.antMatchers("/api/v2/config/getParam").permitAll()
//				.antMatchers("/api/users/industries/**").permitAll()
				.antMatchers("app/api/**")
				.hasAnyRole(RoleBasedAuthority.ROLE_USER.getRole(),
						RoleBasedAuthority.ROLE_ADMIN.getRole())
				.anyRequest().authenticated()
				.and()
				.headers()
				.frameOptions()
				.sameOrigin()
				.and()
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				.and()
				.exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.and()
				.addFilterBefore(this.jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(false);
//			config.addAllowedOrigin("https://dbizpos.digitalbiz.com.vn");
//			config.addAllowedOrigin("https://dbizpos-dev.digitalbiz.com.vn");
//			config.addAllowedOrigin("https://103.98.161.18");
//			config.addAllowedOrigin("http://pos-dev.dbizvn.com");
		config.addAllowedOrigin("*");
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}

