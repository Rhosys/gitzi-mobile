package ch.rhosys.gitzi.data.remote

import ch.rhosys.gitzi.data.remote.dto.AnswerReviewRequest
import ch.rhosys.gitzi.data.remote.dto.BlockTaskRequest
import ch.rhosys.gitzi.data.remote.dto.ConfigDto
import ch.rhosys.gitzi.data.remote.dto.CreateEpicRequest
import ch.rhosys.gitzi.data.remote.dto.CreateTaskRequest
import ch.rhosys.gitzi.data.remote.dto.EpicDto
import ch.rhosys.gitzi.data.remote.dto.ChatMessageDto
import ch.rhosys.gitzi.data.remote.dto.ParkTaskRequest
import ch.rhosys.gitzi.data.remote.dto.ProviderDto
import ch.rhosys.gitzi.data.remote.dto.RejectReviewRequest
import ch.rhosys.gitzi.data.remote.dto.ReviewItemDto
import ch.rhosys.gitzi.data.remote.dto.SendChatRequest
import ch.rhosys.gitzi.data.remote.dto.TaskDto
import ch.rhosys.gitzi.data.remote.dto.UpdateConfigRequest
import ch.rhosys.gitzi.data.remote.dto.UpdateTaskRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST surface of the assumed Gitzi backend — see docs/api-contract.md.
 * Every task/review mutation here corresponds 1:1 to a `gitzi_*` MCP tool
 * the daemon already exposes to agents (mcp/tools.rs); this is the same set
 * of operations, fronted by HTTP for a human operator instead of an agent.
 */
interface GitziApiService {
    @GET("v1/epics")
    suspend fun listEpics(): List<EpicDto>

    @POST("v1/epics")
    suspend fun createEpic(@Body body: CreateEpicRequest): EpicDto

    @GET("v1/tasks")
    suspend fun listTasks(@Query("epic_id") epicId: String? = null): List<TaskDto>

    @POST("v1/tasks")
    suspend fun createTask(@Body body: CreateTaskRequest): TaskDto

    @PATCH("v1/tasks/{taskId}")
    suspend fun updateTask(@Path("taskId") taskId: String, @Body body: UpdateTaskRequest): TaskDto

    @POST("v1/tasks/{taskId}/park")
    suspend fun parkTask(@Path("taskId") taskId: String, @Body body: ParkTaskRequest)

    @POST("v1/tasks/{taskId}/block")
    suspend fun blockTask(@Path("taskId") taskId: String, @Body body: BlockTaskRequest)

    @GET("v1/review-queue")
    suspend fun listReviewQueue(): List<ReviewItemDto>

    @POST("v1/review-queue/{itemId}/answer")
    suspend fun answerReviewItem(@Path("itemId") itemId: String, @Body body: AnswerReviewRequest)

    @POST("v1/review-queue/{itemId}/approve")
    suspend fun approveReviewItem(@Path("itemId") itemId: String)

    @POST("v1/review-queue/{itemId}/reject")
    suspend fun rejectReviewItem(@Path("itemId") itemId: String, @Body body: RejectReviewRequest)

    @GET("v1/chat")
    suspend fun listChat(): List<ChatMessageDto>

    @POST("v1/chat")
    suspend fun sendChatMessage(@Body body: SendChatRequest)

    @GET("v1/config")
    suspend fun getConfig(): ConfigDto

    @PUT("v1/config")
    suspend fun updateConfig(@Body body: UpdateConfigRequest): ConfigDto

    @POST("v1/providers/discover")
    suspend fun discoverProviders(): List<ProviderDto>

    @POST("v1/providers/{name}/activate")
    suspend fun activateProvider(@Path("name") name: String)

    @GET("v1/ping")
    suspend fun ping()
}
