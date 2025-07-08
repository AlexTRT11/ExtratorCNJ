package br.jus.cnj.datajud.elasticToDatajud.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

public class Misc {

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String dateToString(Date date, String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    public static Date stringToDate(String dateStr, String format) {
        if (isEmpty(dateStr)) {
            return null;
        }
        try {
            return new SimpleDateFormat(format).parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException("A data não corresponde ao formato informado");
        }
    }

    public static Date stringToDateTime(String dateTime) {
        if (dateTime == null) {
            throw new RuntimeException("Data da movimentação não informada");
        }
        if (dateTime.length() == 14) {
            return Misc.stringToDate(dateTime, "yyyyMMddHHmmss");
        }
        if (dateTime.length() == 8){
            return Misc.stringToDate(dateTime, "yyyyMMdd");
        }else{
            throw new RuntimeException("Data com tamanho inválido: " + dateTime);
        }
    }

    public static Date millisecondsToDate(Long milliseconds, String format) {
        try {
            DateFormat df = new SimpleDateFormat(format);
            String dateStr = millisecondsToString(milliseconds, format);
            return df.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("A data não corresponde ao formato informado");
        }
    }

    public static String millisecondsToString(Long milliseconds, String format) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(milliseconds);
        return new SimpleDateFormat(format).format(cal.getTime());
    }

    public static boolean isDataMenor(Date data, Date when) {
        return data.before(when);
    }

    public static boolean isDataMaior(Date data, Date when) {
        if (data == null || when == null) {
            return false;
        }
        return data.after(when);
    }

    public static boolean isDataMaiorIgual(Date data, Date when) {
        return data.compareTo(when) >= 0;
    }

    public static boolean isDataMenorIgual(Date data, Date when) {
        return data.compareTo(when) <= 0;
    }

    public static boolean isDataIgual(Date data, Date when) {
        return data.compareTo(when) == 0;
    }

    public static boolean isDataEntre(Date data, Date inferior, Date superior) {
        return isDataMenorIgual(inferior, data)
            && isDataMaiorIgual(superior, data);
    }

    public static Date dataMenosDias(Date data, int nroDias) {
        if (data == null) {
            data = new Date();
        }
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(data);
        gc.add(Calendar.DAY_OF_YEAR, -nroDias);
        return gc.getTime();
    }

    public static Date dataMaisDias(Date data, int nroDias) {
        if (data == null) {
            data = new Date();
        }
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(data);
        gc.add(Calendar.DAY_OF_YEAR, nroDias);
        return gc.getTime();
    }

    public static Date getDiaAnterior(Date data) {
        return dataMenosDias(data, 1);
    }

    public static Date getDiaPosterior(Date data) {
        return dataMaisDias(data, 1);
    }

    public static Integer stringToInteger(String s) {
        if (isEmpty(s)) {
            return null;
        }
        return Integer.parseInt(s);
    }

    public static Long stringToLong(String s) {
        if (isEmpty(s)) {
            return null;
        }
        return Long.parseLong(s);
    }

    public static Double stringToDouble(String s) {
        if (isEmpty(s)) {
            return null;
        }
        return Double.parseDouble(s);
    }

    public static Boolean stringToBoolean(String s) {
        if (isEmpty(s)) {
            return null;
        }
        return Boolean.parseBoolean(s);
    }

    public static boolean isInteger(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isLong(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Long.parseLong(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    
    public static boolean isDouble(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	        Double.parseDouble(strNum);
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}

    public static byte[] stringToByteArray(String str) {
        if (str == null) {
            return null;
        }
        try {
            return str.getBytes("UTF8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return str.getBytes();
    }

    public static String encodeMD5(String text) {
        return encodeMD5(stringToByteArray(text));
    }

    public static String encodeMD5(byte[] bytes) {
        return encode(bytes, "MD5");
    }

    public static String encode(byte[] bytes, String type) {
        StringBuilder resp = new StringBuilder();
        if (bytes != null) {
            try {
                MessageDigest digest = MessageDigest.getInstance(type);
                byte[] hash = digest.digest(bytes);
                for (int i = 0; i < hash.length; i++) {
                    if ((hash[i] & 0xff) < 0x10) {
                        resp.append("0");
                    }
                    resp.append(Long.toString(hash[i] & 0xff, 16));
                }
            } catch (NoSuchAlgorithmException err) {
                err.printStackTrace(System.err);
            }
        }
        return resp.toString();
    }

    public static String completaZeros(String numero, int tamanho) {
        StringBuilder sb = new StringBuilder();
        int zerosAdicionar = tamanho - numero.length();
        while (sb.length() < zerosAdicionar) {
            sb.append('0');
        }
        sb.append(numero);
        return sb.toString();
    }
    
    public static double roundDouble(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = BigDecimal.valueOf(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
    
    public static long getDaysDifference(Integer data1, Integer data2){
        long diffInMillies = (data2 != 0 ? stringToDateTime(data2.toString()).getTime() : new Date().getTime()) - stringToDateTime(data1.toString()).getTime();
        return TimeUnit.DAYS.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }
    
    public static Float convertCoordenate(String position) {
    	Float ret = 0f;
    	if(position == null) {
    		return ret;
    	}else {
    		if(position.contains("°")) {
    			String[] base = position.split("°");
    			if(base.length > 1) {
    				ret += Float.valueOf(base[0]);
    				base = base[1].split("'");
    				if(base.length > 1) {
        				ret += Float.valueOf(base[0])/60;
        				base = base[1].split("\"");
        				if(base.length > 1) {
            				ret += Float.valueOf(base[0])/3600;
            			}
        			}
    			}
    		}else {
    			if(position.contains(",")) {
    				if(position.contains(".")) {
    					position = position.replace(".","");
    				}
    				String[] base = position.split(",");
    				if(base.length > 2) {
    					String saida = "";
    					String sinal = ".";
    					for(String a : base) {
    						saida += a+sinal;
    						sinal = "";
    					}
    					ret = Float.valueOf(saida);
    				}else {
    					ret = Float.valueOf(position.replace(",","."));
    				}    				
    			}else {
    				try {
    					ret = Float.valueOf(position);
    				}
    				catch(Exception e) {
    					
    				}
    			}
    		}
    	}
    	return ret;
    }
    
    public static boolean validarCpf(String cpf) {
    	try {
			String s_aux = cpf;
			if (s_aux.length() == 11) {
				int d1, d2;
				int digito1, digito2, resto;
				int digitoCPF;
				String nDigResult;
				d1 = d2 = 0;
				digito1 = digito2 = resto = 0;
				for (int n_Count = 1; n_Count < s_aux.length() - 1; n_Count++) {
					digitoCPF = Integer.valueOf(s_aux.substring(n_Count - 1, n_Count)).intValue();
					// --------- Multiplique a ultima casa por 2 a seguinte por 3 a seguinte por 4 e
					// assim por diante.
					d1 = d1 + (11 - n_Count) * digitoCPF;
					// --------- Para o segundo digito repita o procedimento incluindo o primeiro
					// digito calculado no passo anterior.
					d2 = d2 + (12 - n_Count) * digitoCPF;
				}
				;
				// --------- Primeiro resto da divisão por 11.
				resto = (d1 % 11);
				// --------- Se o resultado for 0 ou 1 o digito é 0 caso contrário o digito é 11
				// menos o resultado anterior.
				if (resto < 2) {
					digito1 = 0;
				} else {
					digito1 = 11 - resto;
				}
				d2 += 2 * digito1;
				// --------- Segundo resto da divisão por 11.
				resto = (d2 % 11);
				// ---------Se o resultado for 0 ou 1 o digito é 0 caso contrário o digito é 11
				// menos o resultado anterior.
				if (resto < 2) {
					digito2 = 0;
				} else {
					digito2 = 11 - resto;
				}
				// --------- Digito verificador do CPF que está sendo validado.
				String nDigVerific = s_aux.substring(s_aux.length() - 2, s_aux.length());
				// --------- Concatenando o primeiro resto com o segundo.
				nDigResult = String.valueOf(digito1) + String.valueOf(digito2);
				// --------- Comparar o digito verificador do cpf com o primeiro resto + o
				// segundo resto.
				return nDigVerific.equals(nDigResult);
			} else { 
				return false;
			}
    	}catch(Exception e) {
    		return false;
    	}
	}
    
    public static List<JSONObject> jsonArrayToJsonObjectList(JSONArray jsonArray) {
        List<JSONObject> jsonObjectList = null;
        if (jsonArray != null) {
            jsonObjectList = new ArrayList<JSONObject>(0);
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObjectList.add(jsonArray.getJSONObject(i));
            }
        }
        return jsonObjectList;
    }
    
    public static List<JSONObject> elementToJsonObjectList(JSONObject o, String element) {
        List<JSONObject> list = null;
        if (o.has(element)) {
            if (o.get(element) instanceof JSONObject) {
                list = new ArrayList<JSONObject>(0);
                list.add(o.getJSONObject(element));
            } else {
                JSONArray array = o.getJSONArray(element);
                list = Misc.jsonArrayToJsonObjectList(array);
            }
        }
        return list;
    }
}