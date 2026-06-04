<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>
 	<xsl:param name="agendaPessoal" select="'../agendaPessoal.xml'"/>
 	<xsl:param name="calendarioAcademico" select="'../calendarioAcademico.xml'"/>
 	
 	<xsl:variable name="invernoInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/@inicio, '-', ''))"/>
	<xsl:variable name="invernoFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/@fim, '-', ''))"/>
 	<xsl:variable name="invernoNormalInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/normal/@inicio, '-', ''))"/>
	<xsl:variable name="invernoNormalFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/normal/@fim, '-', ''))"/>
 	<xsl:variable name="invernoRecursoInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/recurso/@inicio, '-', ''))"/>
	<xsl:variable name="invernoRecursoFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/recurso/@fim, '-', ''))"/>
 	<xsl:variable name="invernoEspecialInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/especial/@inicio, '-', ''))"/>
	<xsl:variable name="invernoEspecialFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/especial/@fim, '-', ''))"/> 	       
    
    <xsl:variable name="veraoInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/@inicio, '-', ''))"/>
	<xsl:variable name="veraoFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/@fim, '-', ''))"/>
 	<xsl:variable name="veraoNormalInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/normal/@inicio, '-', ''))"/>
	<xsl:variable name="veraoNormalFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/normal/@fim, '-', ''))"/>
 	<xsl:variable name="veraoRecursoInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/recurso/@inicio, '-', ''))"/>
	<xsl:variable name="veraoRecursoFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/recurso/@fim, '-', ''))"/>
 	<xsl:variable name="veraoEspecialInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/especial/@inicio, '-', ''))"/>
	<xsl:variable name="veraoEspecialFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/especial/@fim, '-', ''))"/> 	       
    
    <xsl:variable name="natalInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/natal/@inicio, '-', ''))"/> 	       
    <xsl:variable name="natalFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/inverno/natal/@fim, '-', ''))"/> 	       
    <xsl:variable name="carnavalInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/carnaval/@inicio, '-', ''))"/> 	       
    <xsl:variable name="carnavalFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/carnaval/@fim, '-', ''))"/> 	       
    <xsl:variable name="pascoaInicio" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/pascoa/@inicio, '-', ''))"/> 	       
    <xsl:variable name="pascoaFim" select="number(translate(document($calendarioAcademico)/calendarioAcademico/verao/pascoa/@fim,'-', ''))"/> 	       
    
    <xsl:template match="/">
        <html lang="pt-PT">
        <head>
            <title>Calendário Plurianual</title>
            <style>
                body { font-family: 'Segoe UI', sans-serif; background-color: #f4f7f6; margin: 0; }
                h1 { 
				    color: #004587; 
				    font-size: 26px; 
				    font-weight: 700; 
				    margin-bottom: 5px; 
				    text-transform: uppercase; 
				    text-align: center; 
				}
               .mes-card { 
                    background: white; 
                    width: 100%; 
                    min-height: 100vh; 
                    display: flex;
                    flex-direction: column;
                    break-after: page; 
                }

                .mes-titulo { 
                    background-color: #34495e; color: white; padding: 20px; 
                    text-align: center; font-size: 2em; font-weight: bold; 
                }

                .ano-label { display: block; font-size: 0.5em; color: #bdc3c7; letter-spacing: 2px; }

                table { width: 95%; margin: 40px auto; border-collapse: collapse; table-layout: fixed; }

                th { background-color: #ecf0f1; color: #7f8c8d; padding: 15px; border: 1px solid #ddd; }
                
                td { 
                    height: 100px; /* Altura fixa para parecer um calendário de parede */
                    vertical-align: top; 
                    padding: 10px; 
                    border: 1px solid #ddd; 
                    position: relative;
                    padding-bottom: 20px;
                }

                .num-dia { font-weight: bold; font-size: 1.2em; 
                		   white-space: nowrap; /* Impede que o texto mude de linha */
    					   display: inline-flex; /* Alinha os itens lado a lado perfeitamente */
    					   align-items: center;}
    					   
                .info-dia { font-size: 0.75em; display: block; margin-top: 5px; }
                .emoji-tipo { position: absolute; bottom: 5px; right: 5px; font-size: 1.2em; }

                /* Cores por tipo de dia conforme o esquema */
                .dia-fds { background-color: #fdf2f2; color: #e74c3c; }
                .dia-feriado { background-color: #f1c40f; color: #7f6000; }
                .dia-util { color: #2c3e50; }

               				
				.legenda {
				    padding: 20px;
				    text-align: center;
				    background-color: #f9f9f9;
				    border-top: 1px solid #ddd;
				    margin-top: auto; /* Empurra a legenda para o fim do mes-card se necessário */
				}
				
				.legenda p {
				    margin: 0;
				    display: inline-block; /* Garante que o parágrafo respeita o centro do pai */
				}
				
				/* Estilo base para todas as etiquetas */
				.badge-prio {
				    padding: 3px 8px;
				    border-radius: 12px;
				    font-size: 0.85em;
				    font-weight: bold;
				    display: inline-flex;
				    align-items: center;
				    gap: 4px; /* Espaço entre emoji e texto */
				}
				
				/* Cores específicas */
				.prio-alta {
				    background-color: #ffebee; /* Vermelho muito claro */
				    color: #d32f2f;
				    border: 1px solid #ffcdd2;
				}
				
				.prio-media {
				    background-color: #fff3e0; /* Laranja muito claro */
				    color: #ef6c00;
				    border: 1px solid #ffe0b2;
				}
				
				.prio-baixa {
				    background-color: #e3f2fd; /* Azul muito claro */
				    color: #1976d2;
				    border: 1px solid #bbdefb;
				}
				
				.emoji {
				    font-size: 1.1em; /* Emoji ligeiramente maior que o texto */
				}
				
				/* ============================================================
				   🎨 SISTEMA DE PADRÕES VISUAIS - CALENDÁRIO ACADÉMICO
				   ============================================================ */
				
				/* --- ❄️ 1. SEMESTRE DE INVERNO: Riscas Diagonais Azuis (45deg) --- */
				.p-aulas-inverno { 
				    background: repeating-linear-gradient(
				        45deg, 
				        #e3f2fd, 
				        #e3f2fd 10px, 
				        #ffffff 10px, 
				        #ffffff 20px
				    ) !important;
				    border-top: 5px solid #004587 !important;
				}
				.p-aulas-inverno::after {
				  content: "Semestre de Inverno";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-aulas-inverno:hover::after {
				  display: block;
				}
				
				/* --- ☀️ 2. SEMESTRE DE VERÃO: Riscas Diagonais Verdes (135deg) --- */
				.p-aulas-verao { 
				    background: repeating-linear-gradient(
				        135deg, 
				        #e8f5e9, 
				        #e8f5e9 10px, 
				        #ffffff 10px, 
				        #ffffff 20px
				    ) !important;
				    border-top: 5px solid #2e7d32 !important;
				}
				.p-aulas-verao::after {
				  content: "Semestre de Verão";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-aulas-verao:hover::after {
				  display: block;
				}
				
				/* --- 📝 3. ÉPOCAS DE EXAMES (ROXO) --- */
				
				/* 3.1 Época Normal: Grelha Larga e Suave */
				.p-exames-normal { 
				    background-color: #f3e5f5 !important;
				    background-image: 
				        linear-gradient(45deg, rgba(156, 39, 176, 0.05) 25%, transparent 25%), 
				        linear-gradient(-45deg, rgba(156, 39, 176, 0.05) 25%, transparent 25%);
				    background-size: 16px 16px !important;
				    border: 2px solid #ce93d8 !important;
				    color: #4a148c !important;
				}
				.p-exames-normal::after {
				  content: "Exames: época normal";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-exames-normal:hover::after {
				  display: block;
				}
				
				/* 3.2 Época de Recurso: Grelha Apertada (Original) */
				.p-exames-recurso { 
				    background-color: #f3e5f5 !important;
				    background-image: 
				        linear-gradient(45deg, rgba(156, 39, 176, 0.1) 25%, transparent 25%), 
				        linear-gradient(-45deg, rgba(156, 39, 176, 0.1) 25%, transparent 25%), 
				        linear-gradient(45deg, transparent 75%, rgba(156, 39, 176, 0.1) 75%), 
				        linear-gradient(-45deg, transparent 75%, rgba(156, 39, 176, 0.1) 75%);
				    background-size: 8px 8px !important;
				    border: 2px solid #9c27b0 !important;
				    color: #4a148c !important;
				}
				.p-exames-recurso::after {
				  content: "Exames: época de recurso";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-exames-recurso:hover::after {
				  display: block;
				}
				
				/* 3.3 Época Especial: Invertido / Alto Contraste */
				.p-exames-especial { 
				    background-color: #6a1b9a !important;
				    background-image: radial-gradient(rgba(255, 255, 255, 0.2) 2px, transparent 2px) !important;
				    background-size: 10px 10px !important;
				    color: #ffffff !important;
				    border: 2px solid #4a148c !important;
				    font-weight: bold;
				}
				.p-exames-especial::after {
				  content: "Exames: época especial";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-exames-especial:hover::after {
				  display: block;
				}
				
				/* --- 🏖️ 4. INTERRUPÇÕES E FÉRIAS (LARANJA) --- */
				
				/* 4.1 Férias de Natal: Efeito de Neve */
				.p-ferias-natal { 
				    background-color: #fff3e0 !important;
				    background-image: radial-gradient(#fb8c00 2px, transparent 2px) !important;
				    background-size: 15px 15px !important;
				    color: #e65100 !important;
				    border-top: 5px solid #ff9800 !important;
				}
				.p-ferias-natal::after {
				  content: "Férias: natal";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-ferias-natal:hover::after {
				  display: block;
				}
				
				/* 4.2 Férias de Carnaval: Confetis Dinâmicos */
				.p-ferias-carnaval { 
				    background-color: #fff3e0 !important;
				    background-image: 
				        radial-gradient(circle at 2px 2px, #ffa726 1px, transparent 0),
				        radial-gradient(circle at 6px 6px, #fb8c00 1px, transparent 0);
				    background-size: 12px 12px !important;
				    color: #e65100 !important;
				    border-top: 5px solid #fb8c00 !important;
				}
				.p-ferias-carnaval::after {
				  content: "Férias: carnaval";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-ferias-carnaval:hover::after {
				  display: block;
				}
				/* 4.3 Férias da Páscoa: Padrão Ziguezague/Ovos */
				.p-ferias-pascoa { 
				    background-color: #fff3e0 !important;
				    background-image: 
				        linear-gradient(135deg, #ffe0b2 25%, transparent 25%), 
				        linear-gradient(225deg, #ffe0b2 25%, transparent 25%);
				    background-size: 10px 10px !important;
				    color: #e65100 !important;
				    border-top: 5px solid #ffb74d !important;
				}
				.p-ferias-pascoa::after {
				  content: "Férias: páscoa";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}
				
				.p-ferias-pascoa:hover::after {
				  display: block;
				}
            </style>
        </head>
        <body>
            <h1>♾️ Calendário Plurianual</h1>
            <xsl:for-each select="calendario/ano">
                <xsl:variable name="anoAtual" select="@valor"/>
                <xsl:for-each select="mes">
                    <div class="mes-card">
                        <div class="mes-titulo">
                            <xsl:value-of select="@nome"/>/<xsl:value-of select="$anoAtual"/>
                        </div>
                        
                        <table>
                            <thead>
                                <tr>
                                    <th>Dom</th><th>Seg</th><th>Ter</th><th>Qua</th><th>Qui</th><th>Sex</th><th>Sáb</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <xsl:call-template name="espacos-vazios">
                                        <xsl:with-param name="primeiroDia" select="dia[1]/@dia_semana"/>
                                    </xsl:call-template>
                                    
                                    <xsl:apply-templates select="dia"/>
                                </tr>
                            </tbody>
                        </table>

                        <div class="legenda">
                            <p>
                                <strong>Legenda:</strong><span class="emoji" title="Dia Útil">💼</span>&#160;Dia Útil | <span class="emoji" title="Fim de Semana">🥳</span>&#160;Fim Semana | <span class="emoji" title="Feriado Nacional">&#160;🇵🇹</span> Feriado
                                | <span class="p-aulas-inverno" title="Aulas Semestre de Inverno">___</span>&#160;Inverno
                                | <span class="p-aulas-verao" title="Aulas Semestre de Verão">___</span>&#160;Verão
                                | <span class="p-exames-normal" title="Época Normal"><span class="emoji">🔹</span></span>
                                  <span class="p-exames-recurso" title="Época de Recurso">&#160;<span class="emoji">🔄</span></span> 
                                  <span class="p-exames-especial" title="Época Especial">&#160;<span class="emoji">⭐</span></span>&#160;Exames
                                | <span class="p-ferias-natal" title="Férias de Natal">&#160;<span class="emoji">🎄</span></span> 
                                  <span class="p-ferias-carnaval" title="Férias de Carnaval">&#160;<span class="emoji">🎭</span></span>
                                  <span class="p-ferias-pascoa" title="Férias de Páscoa">&#160;<span class="emoji">🐰</span></span>&#160;Interrupções
                            </p>
                        </div>
                    </div>
                </xsl:for-each>
            </xsl:for-each>
        </body>
        </html>
    </xsl:template>

    <xsl:template match="dia">
        <td>
          <xsl:variable name="dataAlvo" select="concat(../../@valor, '-', format-number(../@id_mes, '00'), '-', format-number(@numero, '00'))"/>
          <xsl:variable name="diaNum" select="number(translate($dataAlvo, '-', ''))"/>
		  <xsl:variable name="inicioNum" select="number(translate($invernoInicio, '-', ''))"/>
		  <xsl:variable name="fimNum" select="number(translate($invernoFim, '-', ''))"/>

          <xsl:attribute name="class">
             <xsl:choose>
                 <xsl:when test="fim-de-semana">dia-fds</xsl:when>
                 <xsl:when test="feriado">dia-feriado</xsl:when>
                 <xsl:otherwise>
                              <xsl:choose>
                 				  <xsl:when test="$diaNum &gt;= $invernoInicio and $diaNum &lt;= $invernoFim">p-aulas-inverno</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $invernoNormalInicio and $diaNum &lt;= $invernoNormalFim">p-exames-normal</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $invernoRecursoInicio and $diaNum &lt;= $invernoRecursoFim">p-exames-recurso</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $invernoEspecialInicio and $diaNum &lt;= $invernoEspecialFim">p-exames-especial</xsl:when>
                 				  
                 				  <xsl:when test="$diaNum &gt;= $veraoInicio and $diaNum &lt;= $veraoFim">p-aulas-verao</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $veraoNormalInicio and $diaNum &lt;= $veraoNormalFim">p-exames-normal</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $veraoRecursoInicio and $diaNum &lt;= $veraoRecursoFim">p-exames-recurso</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $veraoEspecialInicio and $diaNum &lt;= $veraoEspecialFim">p-exames-especial</xsl:when>
                 				  
                 				  <xsl:when test="$diaNum &gt;= $natalInicio and $diaNum &lt;= $natalFim">p-ferias-natal</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $carnavalInicio and $diaNum &lt;= $carnavalFim">p-ferias-carnaval</xsl:when>
                 				  <xsl:when test="$diaNum &gt;= $pascoaInicio and $diaNum &lt;= $pascoaFim">p-ferias-pascoa</xsl:when>
                 				  
                 				  <xsl:otherwise>dia-util</xsl:otherwise>
             				   </xsl:choose>
             	 </xsl:otherwise>
             </xsl:choose>
           </xsl:attribute>
            <span class="num-dia">
            	<xsl:value-of select="@numero"/>&#160;
            	<xsl:if test="feriado">
            	<span class="info-dia" title="{feriado}"><xsl:choose>
				        <xsl:when test="feriado = 'Ano Novo'"><span class="emoji">🎆</span> Ano Novo</xsl:when>
				        <xsl:when test="feriado = 'Terça-feira Carnaval'"><span class="emoji">🎭</span> Carnaval</xsl:when>
				        <xsl:when test="feriado = 'Sexta-feira Santa'"><span class="emoji">🐟</span> Sexta Santa</xsl:when>
				        <xsl:when test="feriado = 'Páscoa'"><span class="emoji">🐰</span> Páscoa</xsl:when>
				        <xsl:when test="feriado = 'Dia da Liberdade'"><span class="emoji">🌺</span> Dia Liberdade</xsl:when>
				        <xsl:when test="feriado = 'Dia do Trabalhador'"><span class="emoji">🛠️</span> 1º Maio</xsl:when>
				        <xsl:when test="feriado = 'Corpo de Deus'"><span class="emoji">🕊️</span> Corpo Deus</xsl:when>
				        <xsl:when test="feriado = 'Dia de Portugal'"><span class="emoji">🎖️</span> Dia de Portugal</xsl:when>
				        <xsl:when test="feriado = 'Assunção de Nossa Senhora'"><span class="emoji">✝️</span> Assunção NS</xsl:when>
				        <xsl:when test="feriado = 'Implantação da República'"><span class="emoji">✊</span> República</xsl:when>
				        <xsl:when test="feriado = 'Todos os Santos'"><span class="emoji">😇</span> Todos Santos</xsl:when>
				        <xsl:when test="feriado = 'Restauração da Independência'"><span class="emoji">⚔️</span> Restauração</xsl:when>
				        <xsl:when test="feriado = 'Imaculada Conceição'"><span class="emoji">🌹</span> Imac. Conc.</xsl:when>
				        <xsl:when test="feriado = 'Natal'"><span class="emoji">🎄</span> Natal</xsl:when>
				     </xsl:choose>
				     </span>
	            </xsl:if>
 			</span>
 			<div class="agenda-container">
                <xsl:for-each select="document($agendaPessoal)//evento[starts-with(inicio, $dataAlvo)]">
                    <span class="agenda-item" title="[{@categoria}] {titulo}"><xsl:choose>
				    <xsl:when test="@categoria = 'Social'"><span class="emoji">🥳</span></xsl:when>
				    <xsl:when test="@categoria = 'Trabalho'"><span class="emoji">💼</span></xsl:when>
				    <xsl:when test="@categoria = 'Saúde'"><span class="emoji">🏥</span></xsl:when>
				    <xsl:when test="@categoria = 'Lazer'"><span class="emoji">🏖️</span></xsl:when>
				    <xsl:when test="@categoria = 'Família'"><span class="emoji">🏠</span></xsl:when>
				    <xsl:when test="@categoria = 'Auto-ajuda'"><span class="emoji">🌱</span></xsl:when>
				    <xsl:when test="@categoria = 'Profissional'"><span class="emoji">👔</span></xsl:when>
				    <xsl:when test="@categoria = 'Financeiro'"><span class="emoji">💶</span></xsl:when>
				    <xsl:when test="@categoria = 'Logística'"><span class="emoji">📦</span></xsl:when>
				    <xsl:when test="@categoria = 'Aula'"><span class="emoji">📖</span></xsl:when>
				    <xsl:when test="@categoria = 'Teste'"><span title="Teste" class="emoji">📝</span></xsl:when>
				    <xsl:when test="@categoria = 'Normal'"><span title="Época Normal" class="emoji">🔹</span></xsl:when>
				    <xsl:when test="@categoria = 'Recurso'"><span title="Época de Recurso" class="emoji">🔄</span></xsl:when>
				    <xsl:when test="@categoria = 'Especial'"><span title="Época Especial" class="emoji">⭐</span></xsl:when>
				    </xsl:choose>&#160;<span class="emoji">⌚</span>&#160;<xsl:value-of select="substring(substring-after(inicio, 'T'), 1, 5)"/>/
				    <xsl:value-of select="substring(substring-after(fim, 'T'), 1, 5)"/><br/>&#160;&#160;&#160;&#160;<span class="emoji">📍</span>&#160;
				    <xsl:value-of select="local"/>
				    <br/></span>
				</xsl:for-each>

                <xsl:for-each select="document($agendaPessoal)//tarefa[prazo = $dataAlvo]">
                    <span class="tarefa-item" title="{descricao}">
                    <xsl:choose>
						<xsl:when test="prioridade = 'Alta'">
						    <span title="Prioridade Alta" class="badge-prio prio-alta">
						        <span class="emoji">🔺</span> Alta
						    </span>
						</xsl:when>
						
						<xsl:when test="prioridade = 'Media'">
						    <span title="Prioridade Média" class="badge-prio prio-media">
						        <span class="emoji">🔸</span> Média
						    </span>
						</xsl:when>
						
						<xsl:when test="prioridade = 'Baixa'">
						    <span title="Prioridade Baixa" class="badge-prio prio-baixa">
						        <span class="emoji">🔹</span> Baixa
						    </span>
						</xsl:when>
				    </xsl:choose>:&#160;
				    <xsl:value-of select="substring(descricao, 1, 20)"/>
				    </span> 
                </xsl:for-each>
            </div>
            
            <span class="emoji-tipo">
                <xsl:choose>
                    <xsl:when test="util"><span class="emoji" title="Dia de Trabalho">💼</span></xsl:when>
                    <xsl:when test="fim-de-semana"><span class="emoji" title="Fim de Semana">🥳</span></xsl:when>
                    <xsl:when test="feriado"><span class="emoji" title="Feriado Nacional">🇵🇹</span></xsl:when>
                </xsl:choose>
            </span>
        </td>

        <xsl:if test="@dia_semana = 'Sabado' and position() != last()">
        <!--  tem que existir mudança de linha entre </tr> e <tr> -->
            <xsl:text disable-output-escaping="yes">&lt;/tr&gt;
            &lt;tr&gt;</xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template name="espacos-vazios">
        <xsl:param name="primeiroDia"/>
        <xsl:variable name="n">
            <xsl:choose>
                <xsl:when test="$primeiroDia = 'Segunda'">1</xsl:when>
                <xsl:when test="$primeiroDia = 'Terca'">2</xsl:when>
                <xsl:when test="$primeiroDia = 'Quarta'">3</xsl:when>
                <xsl:when test="$primeiroDia = 'Quinta'">4</xsl:when>
                <xsl:when test="$primeiroDia = 'Sexta'">5</xsl:when>
                <xsl:when test="$primeiroDia = 'Sabado'">6</xsl:when>
                <xsl:otherwise>0</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        
        <xsl:if test="$n > 0">
            <td class="vazio"></td>
            <xsl:call-template name="espacos-vazios-iter">
                <xsl:with-param name="count" select="$n - 1"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="espacos-vazios-iter">
        <xsl:param name="count"/>
        <xsl:if test="$count > 0">
            <td class="vazio"></td>
            <xsl:call-template name="espacos-vazios-iter">
                <xsl:with-param name="count" select="$count - 1"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>