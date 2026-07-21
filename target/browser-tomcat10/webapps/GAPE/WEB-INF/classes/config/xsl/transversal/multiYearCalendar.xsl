<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>
	<xsl:param name="personalAgenda" select="'../personalAgenda.xml'"/>
	<xsl:param name="academicCalendar" select="'../academicCalendar.xml'"/>

	<xsl:variable name="winterStart" select="number(translate(document($academicCalendar)/academicCalendar/winter/@start, '-', ''))"/>
	<xsl:variable name="winterEnd" select="number(translate(document($academicCalendar)/academicCalendar/winter/@end, '-', ''))"/>
	<xsl:variable name="winterNormalStart" select="number(translate(document($academicCalendar)/academicCalendar/winter/normal/@start, '-', ''))"/>
	<xsl:variable name="winterNormalEnd" select="number(translate(document($academicCalendar)/academicCalendar/winter/normal/@end, '-', ''))"/>
	<xsl:variable name="winterResitStart" select="number(translate(document($academicCalendar)/academicCalendar/winter/resit/@start, '-', ''))"/>
	<xsl:variable name="winterResitEnd" select="number(translate(document($academicCalendar)/academicCalendar/winter/resit/@end, '-', ''))"/>
	<xsl:variable name="winterSpecialStart" select="number(translate(document($academicCalendar)/academicCalendar/winter/special/@start, '-', ''))"/>
	<xsl:variable name="winterSpecialEnd" select="number(translate(document($academicCalendar)/academicCalendar/winter/special/@end, '-', ''))"/>

    <xsl:variable name="summerStart" select="number(translate(document($academicCalendar)/academicCalendar/summer/@start, '-', ''))"/>
	<xsl:variable name="summerEnd" select="number(translate(document($academicCalendar)/academicCalendar/summer/@end, '-', ''))"/>
	<xsl:variable name="summerNormalStart" select="number(translate(document($academicCalendar)/academicCalendar/summer/normal/@start, '-', ''))"/>
	<xsl:variable name="summerNormalEnd" select="number(translate(document($academicCalendar)/academicCalendar/summer/normal/@end, '-', ''))"/>
	<xsl:variable name="summerResitStart" select="number(translate(document($academicCalendar)/academicCalendar/summer/resit/@start, '-', ''))"/>
	<xsl:variable name="summerResitEnd" select="number(translate(document($academicCalendar)/academicCalendar/summer/resit/@end, '-', ''))"/>
	<xsl:variable name="summerSpecialStart" select="number(translate(document($academicCalendar)/academicCalendar/summer/special/@start, '-', ''))"/>
	<xsl:variable name="summerSpecialEnd" select="number(translate(document($academicCalendar)/academicCalendar/summer/special/@end, '-', ''))"/>

    <xsl:variable name="christmasStart" select="number(translate(document($academicCalendar)/academicCalendar/winter/christmas/@start, '-', ''))"/>
    <xsl:variable name="christmasEnd" select="number(translate(document($academicCalendar)/academicCalendar/winter/christmas/@end, '-', ''))"/>
    <xsl:variable name="carnivalStart" select="number(translate(document($academicCalendar)/academicCalendar/summer/carnival/@start, '-', ''))"/>
    <xsl:variable name="carnivalEnd" select="number(translate(document($academicCalendar)/academicCalendar/summer/carnival/@end, '-', ''))"/>
    <xsl:variable name="easterStart" select="number(translate(document($academicCalendar)/academicCalendar/summer/easter/@start, '-', ''))"/>
    <xsl:variable name="easterEnd" select="number(translate(document($academicCalendar)/academicCalendar/summer/easter/@end,'-', ''))"/>

    <xsl:template match="/">
        <html lang="en">
        <head>
            <title>Multi-Year Calendar</title>
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
               .month-card {
                    background: white;
                    width: 100%;
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    break-after: page;
                }

                .month-titulo {
                    background-color: #34495e; color: white; padding: 20px;
                    text-align: center; font-size: 2em; font-weight: bold;
                }

                .ano-label { display: block; font-size: 0.5em; color: #bdc3c7; letter-spacing: 2px; }

                table { width: 95%; margin: 40px auto; border-collapse: collapse; table-layout: fixed; }

                th { background-color: #ecf0f1; color: #7f8c8d; padding: 15px; border: 1px solid #ddd; }

                td {
                    height: 100px; /* Fixed height to resemble to wall calendar */
                    vertical-align: top;
                    padding: 10px;
                    border: 1px solid #ddd;
                    position: relative;
                    padding-bottom: 20px;
                }

                .num-day { font-weight: bold; font-size: 1.2em;
		   white-space: nowrap; /* Impede que o texto mude de linha */
					   display: inline-flex; /* Alinha os itens lado to lado perfeitamente */
					   align-items: center;}

                .info-day { font-size: 0.75em; display: block; margin-top: 5px; }
                .emoji-tipo { position: absolute; bottom: 5px; right: 5px; font-size: 1.2em; }

                /* Colors by day type according to the schema */
                .day-fds { background-color: #fdf2f2; color: #e74c3c; }
                .day-holiday { background-color: #f1c40f; color: #7f6000; }
                .day-workday { color: #2c3e50; }


				.legenda {
				    padding: 20px;
				    text-align: center;
				    background-color: #f9f9f9;
				    border-top: 1px solid #ddd;
				    margin-top: auto; /* Pushes the legend to the end of the month card if needed */
				}

				.legenda p {
				    margin: 0;
				    display: inline-block; /* Ensures the paragraph respects the parent center */
				}

				/* Base style for all labels */
				.badge-prio {
				    padding: 3px 8px;
				    border-radius: 12px;
				    font-size: 0.85em;
				    font-weight: bold;
				    display: inline-flex;
				    align-items: center;
				    gap: 4px; /* Space between emoji and text */
				}

				/* Specific colors */
				.prio-alta {
				    background-color: #ffebee; /* Vermelho muito claro */
				    color: #d32f2f;
				    border: 1px solid #ffcdd2;
				}

				.prio-meday {
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
				    VISUAL PATTERN SYSTEM - ACADEMIC CALENDAR
				   ============================================================ */

				/* ---  1. SEMESTRE DE INVERNO: Riscas Diagonais Azuis (45deg) --- */
				.p-lessons-winter {
				    background: repeating-linear-gradient(
				        45deg,
				        #e3f2fd,
				        #e3f2fd 10px,
				        #ffffff 10px,
				        #ffffff 20px
				    ) !important;
				    border-top: 5px solid #004587 !important;
				}
				.p-lessons-winter::after {
				  content: "Winter Semester";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-lessons-winter:hover::after {
				  display: block;
				}

				/* ---  2. SUMMER SEMESTER: Green daygonal stripes (135deg) --- */
				.p-lessons-summer {
				    background: repeating-linear-gradient(
				        135deg,
				        #e8f5e9,
				        #e8f5e9 10px,
				        #ffffff 10px,
				        #ffffff 20px
				    ) !important;
				    border-top: 5px solid #2e7d32 !important;
				}
				.p-lessons-summer::after {
				  content: "Summer Semester";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-lessons-summer:hover::after {
				  display: block;
				}

				/* ---  3. EXAM PERIODS (PURPLE) --- */

				/* 3.1 Regular Period: Wide soft grid */
				.p-exams-normal {
				    background-color: #f3e5f5 !important;
				    background-image:
				        linear-gradient(45deg, rgba(156, 39, 176, 0.05) 25%, transparent 25%),
				        linear-gradient(-45deg, rgba(156, 39, 176, 0.05) 25%, transparent 25%);
				    background-size: 16px 16px !important;
				    border: 2px solid #ce93d8 !important;
				    color: #4a148c !important;
				}
				.p-exams-normal::after {
				  content: "Exams: regular period";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-exams-normal:hover::after {
				  display: block;
				}

				/* 3.2 Resit Period: Tight grid (Original) */
				.p-exams-resit {
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
				.p-exams-resit::after {
				  content: "Exams: resit period";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-exams-resit:hover::after {
				  display: block;
				}

				/* 3.3 Special Period: Inverted / High Contrast */
				.p-exams-special {
				    background-color: #6a1b9a !important;
				    background-image: radayl-gradient(rgba(255, 255, 255, 0.2) 2px, transparent 2px) !important;
				    background-size: 10px 10px !important;
				    color: #ffffff !important;
				    border: 2px solid #4a148c !important;
				    font-weight: bold;
				}
				.p-exams-special::after {
				  content: "Exams: special period";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-exams-special:hover::after {
				  display: block;
				}

				/* ---  4. BREAKS AND HOLIDAYS (ORANGE) --- */

				/* 4.1 Christmas Break: Snow effect */
				.p-breaks-christmas {
				    background-color: #fff3e0 !important;
				    background-image: radayl-gradient(#fb8c00 2px, transparent 2px) !important;
				    background-size: 15px 15px !important;
				    color: #e65100 !important;
				    border-top: 5px solid #ff9800 !important;
				}
				.p-breaks-christmas::after {
				  content: "Break: Christmas";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-breaks-christmas:hover::after {
				  display: block;
				}

				/* 4.2 Carnival Break: Dynamic confetti */
				.p-breaks-carnival {
				    background-color: #fff3e0 !important;
				    background-image:
				        radayl-gradient(circle at 2px 2px, #ffa726 1px, transparent 0),
				        radayl-gradient(circle at 6px 6px, #fb8c00 1px, transparent 0);
				    background-size: 12px 12px !important;
				    color: #e65100 !important;
				    border-top: 5px solid #fb8c00 !important;
				}
				.p-breaks-carnival::after {
				  content: "Break: Carnival";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-breaks-carnival:hover::after {
				  display: block;
				}
				/* 4.3 Easter Break: Zigzag/egg pattern */
				.p-breaks-easter {
				    background-color: #fff3e0 !important;
				    background-image:
				        linear-gradient(135deg, #ffe0b2 25%, transparent 25%),
				        linear-gradient(225deg, #ffe0b2 25%, transparent 25%);
				    background-size: 10px 10px !important;
				    color: #e65100 !important;
				    border-top: 5px solid #ffb74d !important;
				}
				.p-breaks-easter::after {
				  content: "Break: Easter";
				  display: none;
				  position: absolute;
				  background: #333;
				  color: #fff;
				  padding: 5px;
				  border-radius: 4px;
				}

				.p-breaks-easter:hover::after {
				  display: block;
				}
            </style>
        </head>
        <body>
            <h1> Multi-Year Calendar</h1>
            <xsl:for-each select="calendar/ano">
                <xsl:variable name="anoAtual" select="@value"/>
                <xsl:for-each select="month">
                    <div class="month-card">
                        <div class="month-titulo">
                            <xsl:value-of select="@name"/>/<xsl:value-of select="$anoAtual"/>
                        </div>

                        <table>
                            <thead>
                                <tr>
                                    <th>Sun</th><th>Mon</th><th>Tue</th><th>Wed</th><th>Thu</th><th>Fri</th><th>Sat</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <xsl:call-template name="espacos-vazios">
                                        <xsl:with-param name="firstDay" select="day[1]/@weekday"/>
                                    </xsl:call-template>

                                    <xsl:apply-templates select="day"/>
                                </tr>
                            </tbody>
                        </table>

                        <div class="legenda">
                            <p>
                                <strong>Legend:</strong><span class="emoji" title="Workday"></span>&#160;Workday | <span class="emoji" title="Weekend"></span>&#160;Weekend | <span class="emoji" title="National Holiday">&#160;</span> Holiday
                                | <span class="p-lessons-winter" title="Lessons Winter Semester">___</span>&#160;Winter
                                | <span class="p-lessons-summer" title="Lessons Summer Semester">___</span>&#160;Summer
                                | <span class="p-exams-normal" title="Regular Period"><span class="emoji"></span></span>
                                  <span class="p-exams-resit" title="Resit Period">&#160;<span class="emoji"></span></span>
                                  <span class="p-exams-special" title="Special Period">&#160;<span class="emoji"></span></span>&#160;Exams
                                | <span class="p-breaks-christmas" title="Christmas Break">&#160;<span class="emoji"></span></span>
                                  <span class="p-breaks-carnival" title="Carnival Break">&#160;<span class="emoji"></span></span>
                                  <span class="p-breaks-easter" title="Easter Break">&#160;<span class="emoji"></span></span>&#160;Breaks
                            </p>
                        </div>
                    </div>
                </xsl:for-each>
            </xsl:for-each>
        </body>
        </html>
    </xsl:template>

    <xsl:template match="day">
        <td>
          <xsl:variable name="targetDate" select="concat(../../@value, '-', format-number(../@month_id, '00'), '-', format-number(@number, '00'))"/>
          <xsl:variable name="dayNum" select="number(translate($targetDate, '-', ''))"/>
		  <xsl:variable name="startNum" select="number(translate($winterStart, '-', ''))"/>
		  <xsl:variable name="endNum" select="number(translate($winterEnd, '-', ''))"/>

          <xsl:attribute name="class">
             <xsl:choose>
                 <xsl:when test="weekend">day-fds</xsl:when>
                 <xsl:when test="holiday">day-holiday</xsl:when>
                 <xsl:otherwise>
                              <xsl:choose>
				  <xsl:when test="$dayNum &gt;= $winterStart and $dayNum &lt;= $winterEnd">p-lessons-winter</xsl:when>
				  <xsl:when test="$dayNum &gt;= $winterNormalStart and $dayNum &lt;= $winterNormalEnd">p-exams-normal</xsl:when>
				  <xsl:when test="$dayNum &gt;= $winterResitStart and $dayNum &lt;= $winterResitEnd">p-exams-resit</xsl:when>
				  <xsl:when test="$dayNum &gt;= $winterSpecialStart and $dayNum &lt;= $winterSpecialEnd">p-exams-special</xsl:when>

				  <xsl:when test="$dayNum &gt;= $summerStart and $dayNum &lt;= $summerEnd">p-lessons-summer</xsl:when>
				  <xsl:when test="$dayNum &gt;= $summerNormalStart and $dayNum &lt;= $summerNormalEnd">p-exams-normal</xsl:when>
				  <xsl:when test="$dayNum &gt;= $summerResitStart and $dayNum &lt;= $summerResitEnd">p-exams-resit</xsl:when>
				  <xsl:when test="$dayNum &gt;= $summerSpecialStart and $dayNum &lt;= $summerSpecialEnd">p-exams-special</xsl:when>

				  <xsl:when test="$dayNum &gt;= $christmasStart and $dayNum &lt;= $christmasEnd">p-breaks-christmas</xsl:when>
				  <xsl:when test="$dayNum &gt;= $carnivalStart and $dayNum &lt;= $carnivalEnd">p-breaks-carnival</xsl:when>
				  <xsl:when test="$dayNum &gt;= $easterStart and $dayNum &lt;= $easterEnd">p-breaks-easter</xsl:when>

				  <xsl:otherwise>day-workday</xsl:otherwise>
				   </xsl:choose>
	 </xsl:otherwise>
             </xsl:choose>
           </xsl:attribute>
            <span class="num-day">
	<xsl:value-of select="@number"/>&#160;
	<xsl:if test="holiday">
	<span class="info-day" title="{holiday}"><xsl:choose>
				        <xsl:when test="holiday = 'New Year'"><span class="emoji"></span> New Year</xsl:when>
				        <xsl:when test="holiday = 'Carnival Tuesday'"><span class="emoji"></span> Carnival</xsl:when>
				        <xsl:when test="holiday = 'Good Friday'"><span class="emoji"></span> Good Friday</xsl:when>
				        <xsl:when test="holiday = 'Easter'"><span class="emoji"></span> Easter</xsl:when>
				        <xsl:when test="holiday = 'Freedom Day'"><span class="emoji"></span> Freedom Day</xsl:when>
				        <xsl:when test="holiday = 'Labor Day'"><span class="emoji"></span> May 1</xsl:when>
				        <xsl:when test="holiday = 'Corpus Christi'"><span class="emoji"></span> Corpus Christi</xsl:when>
				        <xsl:when test="holiday = 'Portugal Day'"><span class="emoji"></span> Portugal Day</xsl:when>
				        <xsl:when test="holiday = 'Assumption of Mary'"><span class="emoji"></span> Assumption</xsl:when>
				        <xsl:when test="holiday = 'Republic Day'"><span class="emoji"></span> Republic</xsl:when>
				        <xsl:when test="holiday = 'All Saints Day'"><span class="emoji"></span> All Saints</xsl:when>
				        <xsl:when test="holiday = 'Restoration of Independence'"><span class="emoji"></span> Restoration</xsl:when>
				        <xsl:when test="holiday = 'Immaculate Conception'"><span class="emoji"></span> Immac. Conc.</xsl:when>
				        <xsl:when test="holiday = 'Christmas'"><span class="emoji"></span> Christmas</xsl:when>
				     </xsl:choose>
				     </span>
	            </xsl:if>
			</span>
			<div class="agenda-container">
                <xsl:for-each select="document($personalAgenda)//evento[starts-with(start, $targetDate)]">
                    <span class="agenda-item" title="[{@categoria}] {titulo}"><xsl:choose>
				    <xsl:when test="@categoria = 'Social'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Trabalho'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Health'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Leisure'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Family'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Self-help'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Professional'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Financial'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Logistics'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Lesson'"><span class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Test'"><span title="Test" class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Normal'"><span title="Regular Period" class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Resit'"><span title="Resit Period" class="emoji"></span></xsl:when>
				    <xsl:when test="@categoria = 'Special'"><span title="Special Period" class="emoji"></span></xsl:when>
				    </xsl:choose>&#160;<span class="emoji"></span>&#160;<xsl:value-of select="substring(substring-after(start, 'T'), 1, 5)"/>/
				    <xsl:value-of select="substring(substring-after(end, 'T'), 1, 5)"/><br/>&#160;&#160;&#160;&#160;<span class="emoji"></span>&#160;
				    <xsl:value-of select="local"/>
				    <br/></span>
				</xsl:for-each>

                <xsl:for-each select="document($personalAgenda)//tarefa[prazo = $targetDate]">
                    <span class="tarefa-item" title="{description}">
                    <xsl:choose>
						<xsl:when test="prioridade = 'Alta'">
						    <span title="High Priority" class="badge-prio prio-alta">
						        <span class="emoji"></span> Alta
						    </span>
						</xsl:when>

						<xsl:when test="prioridade = 'Meday'">
						    <span title="Medium Priority" class="badge-prio prio-meday">
						        <span class="emoji"></span> Medium
						    </span>
						</xsl:when>

						<xsl:when test="prioridade = 'Baixa'">
						    <span title="Low Priority" class="badge-prio prio-baixa">
						        <span class="emoji"></span> Baixa
						    </span>
						</xsl:when>
				    </xsl:choose>:&#160;
				    <xsl:value-of select="substring(description, 1, 20)"/>
				    </span>
                </xsl:for-each>
            </div>

            <span class="emoji-tipo">
                <xsl:choose>
                    <xsl:when test="workday"><span class="emoji" title="Workday"></span></xsl:when>
                    <xsl:when test="weekend"><span class="emoji" title="Weekend"></span></xsl:when>
                    <xsl:when test="holiday"><span class="emoji" title="National Holiday"></span></xsl:when>
                </xsl:choose>
            </span>
        </td>

        <xsl:if test="@weekday = 'Saturday' and position() != last()">
        <!--  there must be to line break between </tr> and <tr> -->
            <xsl:text disable-output-escaping="yes">&lt;/tr&gt;
            &lt;tr&gt;</xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template name="espacos-vazios">
        <xsl:param name="firstDay"/>
        <xsl:variable name="n">
            <xsl:choose>
                <xsl:when test="$firstDay = 'Monday'">1</xsl:when>
                <xsl:when test="$firstDay = 'Tuesday'">2</xsl:when>
                <xsl:when test="$firstDay = 'Wednesday'">3</xsl:when>
                <xsl:when test="$firstDay = 'Thursday'">4</xsl:when>
                <xsl:when test="$firstDay = 'Friday'">5</xsl:when>
                <xsl:when test="$firstDay = 'Saturday'">6</xsl:when>
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