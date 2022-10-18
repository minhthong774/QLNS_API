package com.example.springapi.security.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import com.example.springapi.models.Cart;
import com.example.springapi.uploadfile.model.FileDB;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter


@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "user", uniqueConstraints = { @UniqueConstraint(columnNames = "username"),
		@UniqueConstraint(columnNames = "email") })
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)

	private int id;

	@NotBlank
	private String name;

	// @NotBlank
	private String email;

	private String username;

	// @NotBlank
	// @Column(name = "username")
	// private String usernameTemp;

	private String address;


	@Column(name = "remember_token")
	private String rememberToken;

	@Column(name = "created_at")
	private Date createdAt;

	@Column(name = "updated_at")
	private Date updatedAt;

	// @NotBlank
	private String phone;

	// @NotBlank
	private String password;
	
	private String tokenFireBase;
	

	// @NotEmpty
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new HashSet<>();
	@ManyToOne
	@JoinColumn(name = "image_id")
	private FileDB imageUser;

	
	
	// public User() {
	// }


	// @OneToMany(
    //     mappedBy = "user",
    //     cascade = CascadeType.ALL,
    //     orphanRemoval = true
    // )
    // private List<Cart> carts = new ArrayList<>();

	
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPhone() {
        return phone;
    }

	public void setPhone(String phone) {
        this.phone = phone;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	

	public User(int id, @NotBlank String name, String email, String username, String address, String rememberToken,
			Date createdAt, Date updatedAt, Set<Role> roles) { // for sigin up
		this.id = id;
		this.name = name;
		this.email = email;
		this.username = username;
		this.address = address;
		this.rememberToken = rememberToken;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.roles = roles;
	}

	

	public User(int id, @NotBlank String name, String email, String username, String address, String rememberToken,
			Date createdAt, Date updatedAt, String password) { // for register
		this.id = id;
		this.name = name;
		this.email = email;
		this.username = username;
		this.address = address;
		this.rememberToken = rememberToken;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.password = password;
	}

	public User(int id, String newPassword){
		this.id = id;
		this.password = newPassword;
	}

	public User(String username,String phone, String name,String address, String password) {// for login
		this.username = username;
		this.phone = phone;
		this.name = name;
		this.address = address;
		this.password = password;
	}

	public User(int id, @NotBlank String name, String email, String username, String address, String rememberToken,
			Date createdAt, Date updatedAt, String password, Set<Role> roles, FileDB imageUser) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.username = username;
		this.address = address;
		this.rememberToken = rememberToken;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.password = password;
		this.roles = roles;
		this.imageUser = imageUser;
	}
	
	public User(int id, @NotBlank String name, String email, String username, String address, String rememberToken,
			Date createdAt, Date updatedAt, String password, Set<Role> roles, FileDB imageUser, String tokenFireBase) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.username = username;
		this.address = address;
		this.rememberToken = rememberToken;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.password = password;
		this.roles = roles;
		this.imageUser = imageUser;
		this.tokenFireBase = tokenFireBase;
	}

	public User(int id, @NotBlank String name, String email, String username, String address, String rememberToken,
			Date createdAt, Date updatedAt, String password, Set<Role> roles, FileDB imageUser, String tokenFireBase, String phone) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.username = username;
		this.address = address;
		this.rememberToken = rememberToken;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.password = password;
		this.roles = roles;
		this.imageUser = imageUser;
		this.tokenFireBase = tokenFireBase;
		this.phone = phone;
	}

}
