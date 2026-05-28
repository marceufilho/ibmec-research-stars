package br.com.ibmec.researchstars.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Curso oferecido pela instituição.
 *
 * <p>Modelo de domínio: ver Requisitos.md seção 4 (entidade {@code Course}). Cursos são mantidos
 * pelo administrador (RF-25) e referenciados por professores no autocadastro (RF-01, RF-26).
 */
@Entity
@Table(name = "courses")
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String code;

  protected Course() {
    // for JPA
  }

  public Course(String name, String code) {
    this.name = name;
    this.code = code;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Course course)) {
      return false;
    }
    return id != null && Objects.equals(id, course.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

