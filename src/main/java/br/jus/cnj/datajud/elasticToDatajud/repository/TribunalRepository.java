/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.jus.cnj.datajud.elasticToDatajud.repository;

import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author ricardo.nascimento
 */

@Repository
public interface TribunalRepository extends JpaRepository<Tribunal, Integer> {

}
