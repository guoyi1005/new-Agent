import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * 考试链路的来源携带。
 *
 * 从课程详情的「课程考试」进入时，URL 上会带 from=course&courseId=N。
 * 把它沿着「试卷列表 → 答题 → 成绩 → 详情」一路传下去，
 * 每一页的返回就都能回到那门课程，而不是笼统地回到「我的试卷」。
 *
 * 没有来源参数时行为保持不变：返回一律回到「我的试卷」。
 */
export function useExamReturn() {
  const route = useRoute()
  const router = useRouter()

  const returnCourseId = computed(() => (
    route.query.from === 'course' && route.query.courseId ? String(route.query.courseId) : ''
  ))

  /** 转发给下一页的 query；没有来源时是空对象，不影响原有跳转 */
  const examQuery = computed(() => (
    returnCourseId.value ? { from: 'course', courseId: returnCourseId.value } : {}
  ))

  /** 返回来源课程；没有来源时回到「我的试卷」 */
  function goExamBack() {
    if (returnCourseId.value) {
      router.push(`/courses/${returnCourseId.value}`)
      return
    }
    router.push('/mine/papers')
  }

  return { returnCourseId, examQuery, goExamBack }
}
