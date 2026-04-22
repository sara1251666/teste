package bll;

import model.UnidadeCurricular;
import model.Docente;
import model.Curso;
import dal.UcDAL;
import dal.DocenteDAL;
import dal.CursoDAL;

/**
 * Camada de Lógica de Negócio para as Unidades Curriculares.
 * Orquestra a construção (hidratação) dos objetos.
 */
public class UcBLL {

    private static final String PASTA_BD = "bd";

    /**
     * Constrói o objeto UnidadeCurricular completo com as suas dependências.
     */
    public UnidadeCurricular procurarUCCompleta(String sigla) {
        String[] dados = UcDAL.obterDadosBrutosUC(sigla, PASTA_BD);
        if (dados == null) return null;

        try {
            String siglaUc = dados[0].trim();
            String nomeUc = dados[1].trim();
            int ano = Integer.parseInt(dados[2].trim());
            String siglaDocente = dados[3].trim();

             Docente docResponsavel = DocenteDAL.procurarPorSigla(siglaDocente, PASTA_BD);

            UnidadeCurricular uc = new UnidadeCurricular(siglaUc, nomeUc, ano, docResponsavel);

            if (dados.length >= 5 && !dados[4].trim().equalsIgnoreCase("N/A") && !dados[4].trim().isEmpty()) {
                String siglaCurso = dados[4].trim();
                Curso curso = CursoDAL.procurarCurso(siglaCurso, PASTA_BD);
                if (curso != null) {
                    uc.adicionarCurso(curso);
                }
            }

            return uc;

        } catch (NumberFormatException e) {
            System.err.println(">> Erro na BLL ao construir a UC " + sigla + ": ano inválido.");
            return null;
        }
    }
}