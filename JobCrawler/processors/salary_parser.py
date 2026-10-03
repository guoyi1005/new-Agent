from dataclasses import dataclass
from decimal import Decimal
import re


@dataclass(frozen=True)
class ParsedSalary:
    salary_min: Decimal | None
    salary_max: Decimal | None
    salary_unit: str | None
    salary_months: int | None


class SalaryParser:
    RANGE = re.compile(r"(?i)(\d+(?:\.\d+)?)\s*[-~至到]\s*(\d+(?:\.\d+)?)\s*(k|千|元\s*/\s*天|元\s*/\s*日|/天|万(?:\s*/\s*年|\s*年)?)?(?:[^0-9]{0,8}(\d+)\s*薪)?")

    def parse(self, value: str | None) -> ParsedSalary:
        if not value or "面议" in value:
            return ParsedSalary(None, None, None, None)
        match = self.RANGE.search(value.replace(",", ""))
        if not match:
            return ParsedSalary(None, None, None, None)
        unit = (match.group(3) or "month").lower().replace(" ", "")
        multiplier = Decimal(1000) if unit in ("k", "千") else Decimal(10000) if "万" in unit else Decimal(1)
        parsed_unit = "day" if "天" in unit or "日" in unit else "year" if "年" in unit else "month"
        return ParsedSalary(Decimal(match.group(1)) * multiplier, Decimal(match.group(2)) * multiplier,
                            parsed_unit, int(match.group(4)) if match.group(4) else None)
